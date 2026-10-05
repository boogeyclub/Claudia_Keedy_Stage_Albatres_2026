import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
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
  MarketNegotiation
} from '../../../../core/market/market-models';
import {
  MarketStatusTone,
  appointmentStatus,
  conversationStatus,
  isOpenAppointment,
  isOpenNegotiation,
  negotiationStatus
} from '../../../../core/market/market-status';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { MarketStatusChipComponent } from '../../../../shared/market-status-chip/market-status-chip';

/**
 * Messaging workspace shared by the buyer and the seller.
 *
 * The backend exposes one inbox per signed-in account, so the same page serves both roles: it lists
 * the threads, shows the exchange, and drives the negotiation and site-visit steps. Whether an
 * action is allowed (decide versus cancel) is derived from the proposal author, and the server
 * re-checks it on every call.
 */
@Component({
  selector: 'app-market-conversations',
  imports: [ReactiveFormsModule, MarketStatusChipComponent],
  templateUrl: './market-conversations.html',
  styleUrl: './market-conversations.css'
})
export class MarketConversationsComponent implements OnInit {
  protected readonly i18n = inject(TranslationService);
  protected readonly conversations = signal<readonly MarketConversation[]>([]);
  protected readonly isLoadingList = signal(false);
  protected readonly listFailed = signal(false);
  protected readonly detail = signal<ConversationDetailResponse | null>(null);
  protected readonly isLoadingDetail = signal(false);
  protected readonly isBusy = signal(false);
  protected readonly currentUserId = computed(() => this.authSession.user()?.id ?? null);
  protected actionsForm: UntypedFormGroup = new UntypedFormGroup({});

  private readonly marketApi = inject(MarketApiService);
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
    if (conversationId === undefined || !dateProposee) {
      this.notifications.warning({ key: 'market.conversations.appointmentInvalid' });
      return;
    }

    this.run(
      this.marketApi.proposeAppointment(conversationId, {
        dateProposee: new Date(dateProposee).toISOString(),
        lieu: this.optionalText(this.actionsForm.get('lieu')?.value),
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

  protected isSelected(conversation: MarketConversation): boolean {
    return this.detail()?.conversation.id === conversation.id;
  }

  protected isMine(expediteurId: number): boolean {
    return expediteurId === this.currentUserId();
  }

  protected isCounterpart(proposeurId: number): boolean {
    return proposeurId !== this.currentUserId();
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
          const requested = Number(this.route.snapshot.queryParamMap.get('conversation'));
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
