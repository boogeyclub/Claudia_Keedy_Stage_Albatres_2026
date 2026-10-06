import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { finalize } from 'rxjs';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { MarketApiService } from '../../../../core/market/market-api.service';
import { formatDate, formatKilograms, formatPrice } from '../../../../core/market/market-format';
import { LotPayload, MarketLot, MarketReference } from '../../../../core/market/market-models';
import { MarketStatusTone, lotStatus } from '../../../../core/market/market-status';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { LocationPickerComponent } from '../../../../shared/location-picker/location-picker';
import { MarketStatusChipComponent } from '../../../../shared/market-status-chip/market-status-chip';

/**
 * Seller catalogue: publish lots, keep their price and volume up to date, and move them through
 * their life cycle (draft → published → reserved → sold, or archived).
 *
 * A lot becomes RESERVE only when a buyer's negotiation is accepted; the seller never sets that
 * status by hand, which is why the page offers PUBLIE, ARCHIVE and VENDU only.
 */
@Component({
  selector: 'app-seller-lots',
  imports: [ReactiveFormsModule, MarketStatusChipComponent, LocationPickerComponent],
  templateUrl: './seller-lots.html',
  styleUrl: './seller-lots.css'
})
export class SellerLotsComponent implements OnInit {
  protected readonly i18n = inject(TranslationService);
  protected readonly lots = signal<readonly MarketLot[]>([]);
  protected readonly reference = signal<MarketReference | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly loadFailed = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly busyLotId = signal<number | null>(null);
  protected readonly editingLotId = signal<number | null>(null);
  protected readonly isEditorOpen = signal(false);
  protected readonly selectedRegionId = signal<number | null>(null);
  protected lotForm: UntypedFormGroup = new UntypedFormGroup({});

  private readonly marketApi = inject(MarketApiService);
  private readonly notifications = inject(NotificationService);
  private readonly title = inject(Title);
  private readonly formBuilder = inject(UntypedFormBuilder);

  protected readonly villeOptions = computed(() => {
    const reference = this.reference();
    const regionId = this.selectedRegionId();
    if (!reference || !regionId) {
      return [];
    }
    return reference.regions.find((region) => region.id === regionId)?.villes ?? [];
  });

  protected readonly publishedCount = computed(
    () => this.lots().filter((lot) => lot.statut === 'PUBLIE' || lot.statut === 'RESERVE').length
  );

  /**
   * GPS pin of the lot, kept in signals rather than in the form: it is chosen on a map, never typed.
   * The seller can still describe the place in words with the address field.
   */
  protected readonly lotLatitude = signal<number | null>(null);
  protected readonly lotLongitude = signal<number | null>(null);
  protected readonly lotLabel = signal<string | null>(null);

  constructor() {
    effect(() => this.title.setTitle(`${this.i18n.t('common.brandName')} | ${this.i18n.t('market.lots.title')}`));
  }

  ngOnInit(): void {
    this.lotForm = this.formBuilder.group({
      titre: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(2000)]],
      typeCacaoId: [null, [Validators.required]],
      quantiteKg: [null, [Validators.required, Validators.min(0.01)]],
      prixKg: [null, [Validators.required, Validators.min(0.01)]],
      devise: ['XAF', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
      regionId: [null, [Validators.required]],
      villeId: [{ value: null, disabled: true }],
      localisation: ['', [Validators.maxLength(200)]],
      dateRecolte: [null],
      dateDisponibilite: [null],
      photoUrl: ['', [Validators.maxLength(500)]],
      publier: [true]
    });

    this.lotForm.get('regionId')?.valueChanges.subscribe((value: unknown) => {
      this.selectedRegionId.set(this.number(value));
      const villeControl = this.lotForm.get('villeId');
      villeControl?.setValue(null);
      if (this.villeOptions().length > 0) {
        villeControl?.enable();
      } else {
        villeControl?.disable();
      }
    });

    this.loadReference();
    this.loadLots();
  }

  protected startCreate(): void {
    this.editingLotId.set(null);
    this.selectedRegionId.set(null);
    this.lotForm.reset({
      titre: '',
      description: '',
      typeCacaoId: null,
      quantiteKg: null,
      prixKg: null,
      devise: 'XAF',
      regionId: null,
      villeId: null,
      localisation: '',
      dateRecolte: null,
      dateDisponibilite: null,
      photoUrl: '',
      publier: true
    });
    this.lotForm.get('villeId')?.disable();
    this.lotLatitude.set(null);
    this.lotLongitude.set(null);
    this.lotLabel.set(null);
    this.isEditorOpen.set(true);
  }

  protected startEdit(lot: MarketLot): void {
    this.editingLotId.set(lot.id);
    this.selectedRegionId.set(lot.regionId);
    this.lotForm.reset({
      titre: lot.titre,
      description: lot.description ?? '',
      typeCacaoId: lot.typeCacaoId,
      quantiteKg: lot.quantiteKg,
      prixKg: lot.prixKg,
      devise: lot.devise,
      regionId: lot.regionId,
      villeId: lot.villeId,
      localisation: lot.localisation ?? '',
      dateRecolte: lot.dateRecolte,
      dateDisponibilite: lot.dateDisponibilite,
      photoUrl: lot.photoUrl ?? '',
      publier: false
    });
    this.lotLatitude.set(lot.latitude);
    this.lotLongitude.set(lot.longitude);
    this.lotLabel.set(lot.localisation);
    const villeControl = this.lotForm.get('villeId');
    if (this.villeOptions().length > 0) {
      villeControl?.enable();
    }
    this.isEditorOpen.set(true);
  }

  protected closeEditor(): void {
    this.isEditorOpen.set(false);
    this.editingLotId.set(null);
  }

  protected save(): void {
    if (this.isSaving()) {
      return;
    }
    if (this.lotForm.invalid) {
      this.lotForm.markAllAsTouched();
      this.notifications.warning({ key: 'market.lots.formInvalid' });
      return;
    }

    const payload = this.payload();
    const editingId = this.editingLotId();
    this.isSaving.set(true);
    const call = editingId === null
      ? this.marketApi.createLot(payload)
      : this.marketApi.updateLot(editingId, { ...payload, publier: undefined });

    call.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.notifications.success({ key: editingId === null ? 'market.lots.created' : 'market.lots.updated' });
        this.closeEditor();
        this.loadLots();
      },
      error: () => this.notifications.error({ key: 'market.lots.saveError' })
    });
  }

  protected publish(lot: MarketLot): void {
    this.changeStatus(lot, 'PUBLIE', 'market.lots.published');
  }

  protected archive(lot: MarketLot): void {
    this.changeStatus(lot, 'ARCHIVE', 'market.lots.archived');
  }

  protected markSold(lot: MarketLot): void {
    this.changeStatus(lot, 'VENDU', 'market.lots.sold');
  }

  protected canPublish(lot: MarketLot): boolean {
    return lot.statut === 'BROUILLON' || lot.statut === 'ARCHIVE';
  }

  protected canArchive(lot: MarketLot): boolean {
    return lot.statut === 'PUBLIE' || lot.statut === 'RESERVE' || lot.statut === 'BROUILLON';
  }

  protected canSell(lot: MarketLot): boolean {
    return lot.statut === 'PUBLIE' || lot.statut === 'RESERVE';
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

  private changeStatus(lot: MarketLot, statut: string, successKey: string): void {
    if (this.busyLotId() !== null) {
      return;
    }

    this.busyLotId.set(lot.id);
    this.marketApi
      .changeLotStatus(lot.id, statut)
      .pipe(finalize(() => this.busyLotId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success({ key: successKey });
          this.loadLots();
        },
        error: () => this.notifications.error({ key: 'market.lots.statusError' })
      });
  }

  private loadLots(): void {
    this.isLoading.set(true);
    this.loadFailed.set(false);

    this.marketApi
      .myLots()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.lots.set(response.lots),
        error: () => {
          this.loadFailed.set(true);
          this.notifications.error({ key: 'market.lots.loadError' });
        }
      });
  }

  private loadReference(): void {
    this.marketApi.reference().subscribe({
      next: (reference) => this.reference.set(reference),
      error: () => this.notifications.error({ key: 'market.catalogue.referenceError' })
    });
  }

  private payload(): LotPayload {
    const values = this.lotForm.getRawValue() as Record<string, unknown>;
    return {
      titre: String(values['titre'] ?? '').trim(),
      description: this.text(values['description']),
      typeCacaoId: Number(values['typeCacaoId']),
      quantiteKg: Number(values['quantiteKg']),
      prixKg: Number(values['prixKg']),
      devise: String(values['devise'] ?? 'XAF').trim().toUpperCase(),
      regionId: Number(values['regionId']),
      villeId: this.number(values['villeId']),
      localisation: this.text(values['localisation']),
      // The pin comes from the map picker, not from a hand-typed coordinate.
      latitude: this.lotLatitude(),
      longitude: this.lotLongitude(),
      dateRecolte: this.text(values['dateRecolte']),
      dateDisponibilite: this.text(values['dateDisponibilite']),
      photoUrl: this.text(values['photoUrl']),
      publier: Boolean(values['publier'])
    };
  }

  private text(value: unknown): string | null {
    return typeof value === 'string' && value.trim() ? value.trim() : null;
  }

  private number(value: unknown): number | null {
    const parsed = Number(value);
    return value === null || value === undefined || value === '' || Number.isNaN(parsed) ? null : parsed;
  }
}
