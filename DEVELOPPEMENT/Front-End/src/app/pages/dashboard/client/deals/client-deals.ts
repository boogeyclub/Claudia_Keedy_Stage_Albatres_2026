import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { MarketApiService } from '../../../../core/market/market-api.service';
import { formatDateTime, formatKilograms, formatPrice } from '../../../../core/market/market-format';
import { DealsResponse, MarketMutationResponse } from '../../../../core/market/market-models';
import {
  MarketStatusTone,
  appointmentStatus,
  isOpenAppointment,
  isOpenNegotiation,
  negotiationStatus
} from '../../../../core/market/market-status';
import { AuthSessionService } from '../../../../core/auth/auth-session.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { MarketStatusChipComponent } from '../../../../shared/market-status-chip/market-status-chip';

interface DealRow {
  readonly id: number;
  readonly conversationId: number;
  readonly proposeurId: number;
  readonly lotTitre: string;
  readonly contrepartie: string;
  readonly devise: string;
  readonly statusLabelKey: string;
  readonly tone: MarketStatusTone;
  readonly isOpen: boolean;
  readonly detailKey: string;
  readonly detailParams: Record<string, string>;
}

/**
 * Transversal view of the buyer's negotiations and visit requests.
 *
 * It reads `/api/market/deals` instead of opening every thread, so consulting this page never marks
 * the sellers' messages as read.
 */
@Component({
  selector: 'app-client-deals',
  imports: [RouterLink, MarketStatusChipComponent],
  templateUrl: './client-deals.html',
  styleUrl: './client-deals.css'
})
export class ClientDealsComponent implements OnInit {
  protected readonly i18n = inject(TranslationService);
  protected readonly deals = signal<DealsResponse | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly loadFailed = signal(false);
  protected readonly isBusy = signal(false);

  private readonly marketApi = inject(MarketApiService);
  private readonly authSession = inject(AuthSessionService);
  private readonly notifications = inject(NotificationService);
  private readonly title = inject(Title);

  private readonly currentUserId = () => this.authSession.user()?.id ?? null;

  constructor() {
    effect(() => this.title.setTitle(`${this.i18n.t('common.brandName')} | ${this.i18n.t('market.deals.title')}`));
  }

  ngOnInit(): void {
    this.load();
  }

  protected negotiationRows(): readonly DealRow[] {
    return (this.deals()?.negociations ?? []).map((negociation) => ({
      id: negociation.id,
      conversationId: negociation.conversationId ?? 0,
      proposeurId: negociation.proposeurId,
      lotTitre: negociation.lotTitre,
      contrepartie: negociation.contrepartie,
      devise: negociation.lotDevise,
      statusLabelKey: negotiationStatus(negociation.statut).labelKey,
      tone: negotiationStatus(negociation.statut).tone,
      isOpen: isOpenNegotiation(negociation.statut),
      detailKey: 'market.negotiation.rowDetail',
      detailParams: {
        price: formatPrice(negociation.prixKg, negociation.lotDevise, this.i18n.language()),
        quantity: formatKilograms(negociation.quantiteKg, this.i18n.language()),
        expires: formatDateTime(negociation.expiresAt, this.i18n.language())
      }
    }));
  }

  protected appointmentRows(): readonly DealRow[] {
    return (this.deals()?.rendezVous ?? []).map((rendezVous) => ({
      id: rendezVous.id,
      conversationId: rendezVous.conversationId ?? 0,
      proposeurId: rendezVous.proposeurId,
      lotTitre: rendezVous.lotTitre,
      contrepartie: rendezVous.contrepartie,
      devise: rendezVous.lotDevise,
      statusLabelKey: appointmentStatus(rendezVous.statut).labelKey,
      tone: appointmentStatus(rendezVous.statut).tone,
      isOpen: isOpenAppointment(rendezVous.statut),
      detailKey: 'market.appointment.rowDetail',
      detailParams: {
        date: formatDateTime(rendezVous.dateProposee, this.i18n.language()),
        place: rendezVous.lieu ?? '—'
      }
    }));
  }

  protected isCounterpart(proposeurId: number): boolean {
    return proposeurId !== this.currentUserId();
  }

  protected decideNegotiation(row: DealRow, accepted: boolean): void {
    this.run(
      this.marketApi.decideNegotiation(row.id, accepted),
      accepted ? 'market.conversations.negotiationAccepted' : 'market.conversations.negotiationRefused'
    );
  }

  protected cancelNegotiation(row: DealRow): void {
    this.run(this.marketApi.cancelNegotiation(row.id), 'market.conversations.negotiationCancelled');
  }

  protected decideAppointment(row: DealRow, accepted: boolean): void {
    this.run(
      this.marketApi.decideAppointment(row.id, accepted),
      accepted ? 'market.conversations.appointmentAccepted' : 'market.conversations.appointmentRefused'
    );
  }

  protected cancelAppointment(row: DealRow): void {
    this.run(this.marketApi.cancelAppointment(row.id), 'market.conversations.appointmentCancelled');
  }

  protected reload(): void {
    this.load();
  }

  protected kilograms(value: number): string {
    return formatKilograms(value, this.i18n.language());
  }

  private load(): void {
    this.isLoading.set(true);
    this.loadFailed.set(false);

    this.marketApi
      .deals()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (deals) => this.deals.set(deals),
        error: () => {
          this.loadFailed.set(true);
          this.notifications.error({ key: 'market.deals.loadError' });
        }
      });
  }

  private run(call: Observable<MarketMutationResponse>, successKey: string): void {
    if (this.isBusy()) {
      return;
    }

    this.isBusy.set(true);
    call.pipe(finalize(() => this.isBusy.set(false))).subscribe({
      next: () => {
        this.notifications.success({ key: successKey });
        this.load();
      },
      error: () => this.notifications.error({ key: 'market.conversations.actionError' })
    });
  }
}
