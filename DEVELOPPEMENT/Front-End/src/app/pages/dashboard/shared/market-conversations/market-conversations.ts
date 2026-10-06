import { Component, DestroyRef, OnDestroy, OnInit, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, finalize } from 'rxjs';
import { AuthSessionService } from '../../../../core/auth/auth-session.service';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { MarketApiService } from '../../../../core/market/market-api.service';
import { formatDateTime, formatKilograms, formatPrice, suggestedVisitSlot } from '../../../../core/market/market-format';
import {
  ConversationDetailResponse,
  MarketAppointment,
  MarketConversation,
  MarketMutationResponse,
  MarketNegotiation,
  MarketPositionShare,
  MarketRealtimeEvent
} from '../../../../core/market/market-models';
import { MarketRealtimeService } from '../../../../core/market/market-realtime.service';
import {
  MarketStatusTone,
  appointmentStatus,
  conversationStatus,
  isOpenAppointment,
  isOpenNegotiation,
  lotStatus,
  needsPointValidation,
  negotiationStatus,
  positionStatus
} from '../../../../core/market/market-status';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { LocationMapComponent } from '../../../../shared/location-map/location-map';
import { LocationPickerComponent } from '../../../../shared/location-picker/location-picker';
import { MarketStatusChipComponent } from '../../../../shared/market-status-chip/market-status-chip';

/**
 * Messaging workspace shared by the buyer and the seller.
 *
 * The backend exposes one inbox per signed-in account, so the same page serves both roles: it lists
 * the threads on the left, the exchange in the middle and everything that can be acted upon on the
 * right (the article, the shared position, the negotiations, the visits and their GPS pin).
 *
 * Two behaviours deserve a word:
 *
 * <ul>
 *   <li>the page subscribes to the market stream while it is open, so an incoming message appears
 *       without any refresh, and a discreet toast names the sender when the thread is not the one
 *       being read;</li>
 *   <li>the pin of a visit is not a decorative detail: it is proposed on a map, approved by each
 *       participant, and the visit only becomes "acté" once both approvals are in. The buttons below
 *       reflect exactly that rule, and the server enforces it again.</li>
 * </ul>
 */
@Component({
  selector: 'app-market-conversations',
  imports: [ReactiveFormsModule, MarketStatusChipComponent, LocationMapComponent, LocationPickerComponent],
  templateUrl: './market-conversations.html',
  styleUrl: './market-conversations.css'
})
export class MarketConversationsComponent implements OnInit, OnDestroy {
  protected readonly i18n = inject(TranslationService);
  protected readonly conversations = signal<readonly MarketConversation[]>([]);
  protected readonly isLoadingList = signal(false);
  protected readonly listFailed = signal(false);
  protected readonly detail = signal<ConversationDetailResponse | null>(null);
  protected readonly isLoadingDetail = signal(false);
  protected readonly isBusy = signal(false);
  protected readonly liveState = inject(MarketRealtimeService).state;
  protected readonly currentUserId = computed(() => this.authSession.user()?.id ?? null);
  protected actionsForm: UntypedFormGroup = new UntypedFormGroup({});

  /** Pin of the visit being proposed, and pin of the position being shared: two separate signs. */
  protected readonly visitLatitude = signal<number | null>(null);
  protected readonly visitLongitude = signal<number | null>(null);
  protected readonly visitLabel = signal<string | null>(null);
  protected readonly shareLatitude = signal<number | null>(null);
  protected readonly shareLongitude = signal<number | null>(null);
  protected readonly shareLabel = signal<string | null>(null);
  protected readonly isSharingPosition = signal(false);
  /** Visits whose pin the current user is moving (only the author may move it). */
  protected readonly movingPointFor = signal<number | null>(null);

  private readonly destroyRef = inject(DestroyRef);
  private readonly marketApi = inject(MarketApiService);
  private readonly realtime = inject(MarketRealtimeService);
  private readonly authSession = inject(AuthSessionService);
  private readonly notifications = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly formBuilder = inject(UntypedFormBuilder);

  /** The proposal still waiting for an answer, if any: the backend allows only one per thread. */
  protected readonly openNegotiation = computed<MarketNegotiation | null>(
    () => this.detail()?.negociations.find((negociation) => isOpenNegotiation(negociation.statut)) ?? null
  );

  protected readonly openAppointment = computed<MarketAppointment | null>(
    () => this.detail()?.rendezVous.find((rendezVous) => isOpenAppointment(rendezVous.statut)) ?? null
  );

  protected readonly positionShare = computed<MarketPositionShare | null>(() => this.detail()?.position ?? null);

  /** The buyer asks, the seller answers: the role decides which card the right panel shows. */
  protected readonly isBuyer = computed(() => {
    const conversation = this.detail()?.conversation;
    return conversation !== undefined && conversation.clientId === this.currentUserId();
  });

  protected readonly canRequestPosition = computed(
    () => this.isBuyer() && this.positionShare() === null && this.canPropose()
  );

  protected readonly mustAnswerPosition = computed(() => {
    const share = this.positionShare();
    if (!share || share.statut !== 'DEMANDE') {
      return false;
    }
    return share.destinataireId === this.currentUserId();
  });

  protected readonly canPropose = computed(() => {
    const status = this.detail()?.conversation.statut;
    return status !== 'ACCORD' && status !== 'CLOTUREE';
  });

  constructor() {
    effect(() => this.title.setTitle(`${this.i18n.t('common.brandName')} | ${this.i18n.t('market.conversations.title')}`));
  }

  ngOnInit(): void {
    this.actionsForm = this.formBuilder.group({
      contenu: ['', [Validators.required, Validators.maxLength(2000)]],
      prixKg: [null, [Validators.required, Validators.min(0.01)]],
      quantiteKg: [null, [Validators.required, Validators.min(0.01)]],
      negotiationMessage: ['', [Validators.maxLength(500)]],
      dateProposee: [suggestedVisitSlot(), [Validators.required]],
      lieu: ['', [Validators.maxLength(200)]],
      note: ['', [Validators.maxLength(500)]]
    });

    this.loadConversations();
    this.realtime.connect();
    // The stream lives in a root service, so the subscription must die with the screen.
    this.realtime.stream
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.handleRealtimeEvent(event));
  }

  ngOnDestroy(): void {
    this.realtime.disconnect();
  }

  protected selectConversation(conversationId: number): void {
    this.isLoadingDetail.set(true);
    this.marketApi
      .conversation(conversationId)
      .pipe(finalize(() => this.isLoadingDetail.set(false)))
      .subscribe({
        next: (detail) => {
          this.detail.set(detail);
          this.actionsForm.get('prixKg')?.setValue(detail.conversation.lotPrixKg);
          this.actionsForm.get('quantiteKg')?.setValue(null);
          this.scrollToLatest();
          void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { conversation: conversationId },
            queryParamsHandling: 'merge',
            replaceUrl: true
          });
        },
        error: () => this.notifications.error({ key: 'market.conversations.threadError' })
      });
  }

  protected reload(): void {
    const selected = this.detail()?.conversation.id;
    this.loadConversations();
    if (selected !== undefined) {
      this.selectConversation(selected);
    }
  }

  protected sendMessage(): void {
    const conversationId = this.detail()?.conversation.id;
    const contenu = this.requiredText(this.actionsForm.get('contenu')?.value);
    if (conversationId === undefined || !contenu) {
      this.notifications.warning({ key: 'market.conversations.messageRequired' });
      return;
    }

    this.run(
      this.marketApi.sendMessage(conversationId, contenu),
      'market.conversations.messageSent',
      () => {
        this.actionsForm.get('contenu')?.setValue('');
        this.selectConversation(conversationId);
      }
    );
  }

  protected proposeNegotiation(): void {
    const conversationId = this.detail()?.conversation.id;
    const prixKg = this.amount(this.actionsForm.get('prixKg')?.value);
    const quantiteKg = this.amount(this.actionsForm.get('quantiteKg')?.value);
    if (conversationId === undefined || prixKg === null || quantiteKg === null) {
      this.notifications.warning({ key: 'market.conversations.negotiationInvalid' });
      return;
    }

    this.run(
      this.marketApi.proposeNegotiation(conversationId, {
        prixKg,
        quantiteKg,
        message: this.optionalText(this.actionsForm.get('negotiationMessage')?.value)
      }),
      'market.conversations.negotiationSent',
      () => {
        this.actionsForm.get('negotiationMessage')?.setValue('');
        this.selectConversation(conversationId);
      }
    );
  }

  protected decideNegotiation(negociation: MarketNegotiation, accepted: boolean): void {
    const conversationId = this.detail()?.conversation.id;
    this.run(
      this.marketApi.decideNegotiation(negociation.id, accepted),
      accepted ? 'market.conversations.negotiationAccepted' : 'market.conversations.negotiationRefused',
      () => conversationId !== undefined && this.selectConversation(conversationId)
    );
  }

  protected cancelNegotiation(negociation: MarketNegotiation): void {
    const conversationId = this.detail()?.conversation.id;
    this.run(
      this.marketApi.cancelNegotiation(negociation.id),
      'market.conversations.negotiationCancelled',
      () => conversationId !== undefined && this.selectConversation(conversationId)
    );
  }

  protected proposeAppointment(): void {
    const conversationId = this.detail()?.conversation.id;
    const dateProposee = this.requiredText(this.actionsForm.get('dateProposee')?.value);
    const latitude = this.visitLatitude();
    const longitude = this.visitLongitude();
    if (conversationId === undefined || !dateProposee) {
      this.notifications.warning({ key: 'market.conversations.appointmentInvalid' });
      return;
    }
    if (latitude === null || longitude === null) {
      // The visit is useless without a place both sides can reach: the pin is mandatory.
      this.notifications.warning({ key: 'market.conversations.appointmentPointRequired' });
      return;
    }

    this.run(
      this.marketApi.proposeAppointment(conversationId, {
        dateProposee: new Date(dateProposee).toISOString(),
        lieu: this.optionalText(this.actionsForm.get('lieu')?.value),
        lieuLibelle: this.optionalText(this.visitLabel()),
        latitude,
        longitude,
        note: this.optionalText(this.actionsForm.get('note')?.value)
      }),
      'market.conversations.appointmentSent',
      () => {
        this.actionsForm.get('note')?.setValue('');
        this.selectConversation(conversationId);
      }
    );
  }

  protected decideAppointment(rendezVous: MarketAppointment, accepted: boolean): void {
    const conversationId = this.detail()?.conversation.id;
    this.run(
      this.marketApi.decideAppointment(rendezVous.id, accepted),
      accepted ? 'market.conversations.appointmentAccepted' : 'market.conversations.appointmentRefused',
      () => conversationId !== undefined && this.selectConversation(conversationId)
    );
  }

  protected cancelAppointment(rendezVous: MarketAppointment): void {
    const conversationId = this.detail()?.conversation.id;
    this.run(
      this.marketApi.cancelAppointment(rendezVous.id),
      'market.conversations.appointmentCancelled',
      () => conversationId !== undefined && this.selectConversation(conversationId)
    );
  }

  /** Approves the pinned place, or refuses it: both approvals are required to confirm the visit. */
  protected decideAppointmentPoint(rendezVous: MarketAppointment, approved: boolean): void {
    this.runDetail(
      this.marketApi.decideAppointmentPoint(rendezVous.id, approved),
      approved ? 'market.conversations.pointApproved' : 'market.conversations.pointRefused'
    );
  }

  protected togglePointMove(rendezVous: MarketAppointment): void {
    const moving = this.movingPointFor() === rendezVous.id;
    this.movingPointFor.set(moving ? null : rendezVous.id);
    this.visitLatitude.set(rendezVous.latitude);
    this.visitLongitude.set(rendezVous.longitude);
    this.visitLabel.set(rendezVous.lieuLibelle);
  }

  /** Saves a moved pin: the invited participant will have to approve it again. */
  protected saveAppointmentPoint(rendezVous: MarketAppointment): void {
    const latitude = this.visitLatitude();
    const longitude = this.visitLongitude();
    if (latitude === null || longitude === null) {
      this.notifications.warning({ key: 'market.conversations.appointmentPointRequired' });
      return;
    }

    this.runDetail(
      this.marketApi.moveAppointmentPoint(rendezVous.id, {
        latitude,
        longitude,
        libelle: this.optionalText(this.visitLabel())
      }),
      'market.conversations.pointMoved',
      () => this.movingPointFor.set(null)
    );
  }

  /** Buyer side: ask the seller to share where the lot really is. */
  protected requestPositionShare(): void {
    const conversationId = this.detail()?.conversation.id;
    if (conversationId === undefined) {
      return;
    }
    this.isSharingPosition.set(true);
    this.marketApi
      .requestPositionShare(conversationId, null)
      .pipe(finalize(() => this.isSharingPosition.set(false)))
      .subscribe({
        next: (detail) => {
          this.detail.set(detail);
          this.notifications.success({ key: 'market.conversations.positionRequested' });
        },
        error: () => this.notifications.error({ key: 'market.conversations.actionError' })
      });
  }

  /** Seller side: share the exact point, or refuse the request. */
  protected answerPositionShare(accepted: boolean): void {
    const share = this.positionShare();
    if (!share) {
      return;
    }
    const latitude = this.shareLatitude();
    const longitude = this.shareLongitude();
    if (accepted && (latitude === null || longitude === null)) {
      this.notifications.warning({ key: 'market.conversations.positionPointRequired' });
      return;
    }

    this.isSharingPosition.set(true);
    this.marketApi
      .decidePositionShare(share.id, accepted, accepted ? {
        latitude: latitude as number,
        longitude: longitude as number,
        libelle: this.optionalText(this.shareLabel())
      } : null)
      .pipe(finalize(() => this.isSharingPosition.set(false)))
      .subscribe({
        next: (detail) => {
          this.detail.set(detail);
          if (accepted) {
            this.shareLatitude.set(null);
            this.shareLongitude.set(null);
            this.shareLabel.set(null);
          }
          this.notifications.success({
            key: accepted ? 'market.conversations.positionShared' : 'market.conversations.positionRefused'
          });
        },
        error: () => this.notifications.error({ key: 'market.conversations.actionError' })
      });
  }

  /** Seller side: withdraw an already shared position. */
  protected revokePositionShare(): void {
    const share = this.positionShare();
    if (!share) {
      return;
    }
    this.isSharingPosition.set(true);
    this.marketApi
      .revokePositionShare(share.id)
      .pipe(finalize(() => this.isSharingPosition.set(false)))
      .subscribe({
        next: (detail) => {
          this.detail.set(detail);
          this.notifications.success({ key: 'market.conversations.positionRevoked' });
        },
        error: () => this.notifications.error({ key: 'market.conversations.actionError' })
      });
  }

  protected isSelected(conversation: MarketConversation): boolean {
    return this.detail()?.conversation.id === conversation.id;
  }

  protected isMine(expediteurId: number): boolean {
    return expediteurId === this.currentUserId();
  }

  protected isCounterpart(proposeurId: number): boolean {
    return proposeurId !== this.currentUserId();
  }

  /** True when the current user may approve or refuse the pin of that visit. */
  protected canDecidePoint(rendezVous: MarketAppointment): boolean {
    return needsPointValidation(rendezVous);
  }

  protected isPointWaitingForMe(rendezVous: MarketAppointment): boolean {
    const userId = this.currentUserId();
    if (!needsPointValidation(rendezVous) || userId === null) {
      return false;
    }
    const isAuthor = rendezVous.proposeurId === userId;
    return isAuthor
      ? rendezVous.pointValideProposeurAt === null
      : rendezVous.pointValideInviteAt === null;
  }

  protected counterpartLabel(conversation: MarketConversation): string {
    return conversation.clientId === this.currentUserId()
      ? `${conversation.vendeurPrenom} ${conversation.vendeurNom}`
      : `${conversation.clientPrenom} ${conversation.clientNom}`;
  }

  protected conversationStatusLabel(status: string): string {
    return conversationStatus(status).labelKey;
  }

  protected conversationStatusTone(status: string): MarketStatusTone {
    return conversationStatus(status).tone;
  }

  protected negotiationStatusLabel(status: string): string {
    return negotiationStatus(status).labelKey;
  }

  protected negotiationStatusTone(status: string): MarketStatusTone {
    return negotiationStatus(status).tone;
  }

  protected appointmentStatusLabel(status: string): string {
    return appointmentStatus(status).labelKey;
  }

  protected appointmentStatusTone(status: string): MarketStatusTone {
    return appointmentStatus(status).tone;
  }

  protected lotStatusLabel(status: string): string {
    return lotStatus(status).labelKey;
  }

  protected lotStatusTone(status: string): MarketStatusTone {
    return lotStatus(status).tone;
  }

  protected positionStatusLabel(status: string): string {
    return positionStatus(status).labelKey;
  }

  protected positionStatusTone(status: string): MarketStatusTone {
    return positionStatus(status).tone;
  }

  protected price(value: number | null | undefined, devise: string | null | undefined): string {
    return formatPrice(value, devise, this.i18n.language());
  }

  protected kilograms(value: number | null | undefined): string {
    return formatKilograms(value, this.i18n.language());
  }

  protected dateTime(value: string | null | undefined): string {
    return formatDateTime(value, this.i18n.language());
  }

  private loadConversations(): void {
    this.isLoadingList.set(true);
    this.listFailed.set(false);

    this.marketApi
      .conversations()
      .pipe(finalize(() => this.isLoadingList.set(false)))
      .subscribe({
        next: (response) => {
          this.conversations.set(response.conversations);
          const requested = Number(this.route.snapshot.queryParamMap.get('conversationId'));
          const fallback = response.conversations[0]?.id;
          const target = response.conversations.some((conversation) => conversation.id === requested)
            ? requested
            : fallback;
          if (target !== undefined) {
            this.selectConversation(target);
          }
        },
        error: () => {
          this.listFailed.set(true);
          this.notifications.error({ key: 'market.conversations.listError' });
        }
      });
  }

  /**
   * Applies one live event: the inbox is refreshed in every case, the open thread is reloaded when the
   * event belongs to it, and a toast names the sender when it does not — except for the message the
   * current user just sent, which is already on screen.
   */
  private handleRealtimeEvent(event: MarketRealtimeEvent): void {
    const isOpenThread = this.detail()?.conversation.id === event.conversationId;
    const isOwnAction = event.authorId === this.currentUserId();

    this.marketApi.conversations().subscribe({
      next: (response) => this.conversations.set(response.conversations),
      error: () => undefined
    });

    if (isOpenThread) {
      this.loadThread(event.conversationId);
      return;
    }
    if (isOwnAction) {
      return;
    }

    // Another thread changed: the inbox badge already moved, the toast says why.
    this.notifications.info({
      key: 'market.conversations.liveEvent',
      params: { lot: event.lotTitre, author: event.authorName, preview: event.preview }
    });
  }

  /**
   * Reloads one thread without disturbing the screen: no spinner and no title change, because the
   * event arrives while the participant is reading.
   */
  private loadThread(conversationId: number): void {
    this.marketApi.conversation(conversationId).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.scrollToLatest();
      },
      error: () => undefined
    });
  }

  /** Keeps the newest message in view while a conversation is being read. */
  private scrollToLatest(): void {
    window.setTimeout(() => {
      const host = document.querySelector<HTMLElement>('[data-message-list]');
      host?.scrollTo({ top: host.scrollHeight, behavior: 'smooth' });
    });
  }

  /**
   * Runs an action whose response is the updated thread: the detail is replaced straight away, so the
   * screen never shows a stale pin or a stale status while waiting for a second round trip.
   */
  private runDetail(
    call: Observable<ConversationDetailResponse>,
    successKey: string,
    onSuccess: () => void = () => undefined
  ): void {
    if (this.isBusy()) {
      return;
    }

    this.isBusy.set(true);
    call.pipe(finalize(() => this.isBusy.set(false))).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.notifications.success({ key: successKey });
        onSuccess();
      },
      error: () => this.notifications.error({ key: 'market.conversations.actionError' })
    });
  }

  private run(
    call: Observable<MarketMutationResponse>,
    successKey: string,
    onSuccess: () => void
  ): void {
    if (this.isBusy()) {
      return;
    }

    this.isBusy.set(true);
    call.pipe(finalize(() => this.isBusy.set(false))).subscribe({
      next: () => {
        this.notifications.success({ key: successKey });
        onSuccess();
      },
      error: () => this.notifications.error({ key: 'market.conversations.actionError' })
    });
  }

  private requiredText(value: unknown): string | null {
    return typeof value === 'string' && value.trim() ? value.trim() : null;
  }

  private optionalText(value: unknown): string | null {
    return this.requiredText(value);
  }

  private amount(value: unknown): number | null {
    const parsed = Number(value);
    return value === null || value === undefined || value === '' || Number.isNaN(parsed) ? null : parsed;
  }
}
