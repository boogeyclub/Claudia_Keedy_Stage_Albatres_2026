import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { MarketApiService } from '../../../../core/market/market-api.service';
import { formatDate, formatKilograms, formatPrice } from '../../../../core/market/market-format';
import {
  CatalogueFilters,
  LotDetailResponse,
  MarketLot,
  MarketReference
} from '../../../../core/market/market-models';
import { MarketStatusTone, lotStatus } from '../../../../core/market/market-status';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { MarketStatusChipComponent } from '../../../../shared/market-status-chip/market-status-chip';

/**
 * Buyer catalogue: filter the published lots, open one and contact its seller.
 *
 * Opening the conversation reuses the single thread the backend keeps for one lot and one buyer, so
 * a second contact never creates a duplicate conversation.
 */
@Component({
  selector: 'app-catalogue',
  imports: [ReactiveFormsModule, MarketStatusChipComponent],
  templateUrl: './catalogue.html',
  styleUrl: './catalogue.css'
})
export class CatalogueComponent implements OnInit {
  protected readonly i18n = inject(TranslationService);
  protected readonly lots = signal<readonly MarketLot[]>([]);
  protected readonly reference = signal<MarketReference | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly loadFailed = signal(false);
  protected readonly selected = signal<LotDetailResponse | null>(null);
  protected readonly isLoadingDetail = signal(false);
  protected readonly isSending = signal(false);
  protected readonly selectedRegionId = signal<number | null>(null);
  protected filtersForm: UntypedFormGroup = new UntypedFormGroup({});

  private readonly marketApi = inject(MarketApiService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly formBuilder = inject(UntypedFormBuilder);

  /** Cities of the selected region; the city selector stays disabled until one is chosen. */
  protected readonly villeOptions = computed(() => {
    const reference = this.reference();
    const regionId = this.selectedRegionId();
    if (!reference || !regionId) {
      return [];
    }
    return reference.regions.find((region) => region.id === regionId)?.villes ?? [];
  });

  constructor() {
    effect(() => this.title.setTitle(`${this.i18n.t('common.brandName')} | ${this.i18n.t('market.catalogue.title')}`));
  }

  ngOnInit(): void {
    this.filtersForm = this.formBuilder.group({
      recherche: [''],
      regionId: [null],
      villeId: [{ value: null, disabled: true }],
      typeCacaoId: [null],
      recolteFrom: [null],
      recolteTo: [null],
      disponibiliteFrom: [null],
      prixMax: [null],
      quantiteMin: [null],
      message: ['']
    });

    this.filtersForm.get('regionId')?.valueChanges.subscribe((value: unknown) => {
      this.selectedRegionId.set(this.number(value));
      const villeControl = this.filtersForm.get('villeId');
      villeControl?.setValue(null);
      if (this.villeOptions().length > 0) {
        villeControl?.enable();
      } else {
        villeControl?.disable();
      }
    });

    this.loadReference();
    this.search();
  }

  protected search(): void {
    this.selected.set(null);
    this.isLoading.set(true);
    this.loadFailed.set(false);

    const values = this.filtersForm.value as Record<string, unknown>;
    const filters: CatalogueFilters = {
      recherche: this.text(values['recherche']),
      regionId: this.number(values['regionId']),
      villeId: this.number(values['villeId']),
      typeCacaoId: this.number(values['typeCacaoId']),
      recolteFrom: this.text(values['recolteFrom']),
      recolteTo: this.text(values['recolteTo']),
      disponibiliteFrom: this.text(values['disponibiliteFrom']),
      prixMax: this.number(values['prixMax']),
      quantiteMin: this.number(values['quantiteMin'])
    };

    this.marketApi
      .catalogue(filters)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.lots.set(response.lots),
        error: () => {
          this.lots.set([]);
          this.loadFailed.set(true);
          this.notifications.error({ key: 'market.catalogue.loadError' });
        }
      });
  }

  protected resetFilters(): void {
    this.selectedRegionId.set(null);
    this.filtersForm.reset({
      recherche: '',
      regionId: null,
      villeId: null,
      typeCacaoId: null,
      recolteFrom: null,
      recolteTo: null,
      disponibiliteFrom: null,
      prixMax: null,
      quantiteMin: null,
      message: ''
    });
    this.filtersForm.get('villeId')?.disable();
    this.search();
  }

  protected openLot(lotId: number): void {
    this.isLoadingDetail.set(true);
    this.marketApi
      .lot(lotId)
      .pipe(finalize(() => this.isLoadingDetail.set(false)))
      .subscribe({
        next: (detail) => this.selected.set(detail),
        error: () => this.notifications.error({ key: 'market.catalogue.lotError' })
      });
  }

  protected closeLot(): void {
    this.selected.set(null);
    this.filtersForm.get('message')?.setValue('');
  }

  protected contactSeller(): void {
    const detail = this.selected();
    if (!detail || this.isSending()) {
      return;
    }

    const message = this.text(this.filtersForm.get('message')?.value);
    this.isSending.set(true);
    this.marketApi
      .openConversation(detail.lot.id, message)
      .pipe(finalize(() => this.isSending.set(false)))
      .subscribe({
        next: (conversation) => {
          this.notifications.success({ key: 'market.catalogue.contactSent' });
          void this.router.navigate(['/dashboard/client/messages'], {
            queryParams: { conversation: conversation.conversation.id }
          });
        },
        error: () => this.notifications.error({ key: 'market.catalogue.contactError' })
      });
  }

  protected lotLabel(lot: MarketLot): string {
    return lot.villeNom ? `${lot.regionNom} · ${lot.villeNom}` : lot.regionNom;
  }

  protected lotStatusLabel(status: string): string {
    return lotStatus(status).labelKey;
  }

  protected lotStatusTone(status: string): MarketStatusTone {
    return lotStatus(status).tone;
  }

  protected price(value: number | null | undefined, devise: string | null | undefined): string {
    return formatPrice(value, devise, this.i18n.language());
  }

  protected kilograms(value: number | null | undefined): string {
    return formatKilograms(value, this.i18n.language());
  }

  protected date(value: string | null | undefined): string {
    return formatDate(value, this.i18n.language());
  }

  private loadReference(): void {
    this.marketApi.reference().subscribe({
      next: (reference) => this.reference.set(reference),
      error: () => this.notifications.error({ key: 'market.catalogue.referenceError' })
    });
  }

  private text(value: unknown): string | null {
    return typeof value === 'string' && value.trim() ? value.trim() : null;
  }

  private number(value: unknown): number | null {
    const parsed = Number(value);
    return value === null || value === undefined || value === '' || Number.isNaN(parsed) ? null : parsed;
  }
}
