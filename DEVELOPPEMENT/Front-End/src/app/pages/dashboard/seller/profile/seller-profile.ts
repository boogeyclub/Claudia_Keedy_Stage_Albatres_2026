import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthSessionService } from '../../../../core/auth/auth-session.service';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { MarketApiService } from '../../../../core/market/market-api.service';
import { formatKilograms, formatPrice } from '../../../../core/market/market-format';
import { MarketLot } from '../../../../core/market/market-models';
import { lotStatus } from '../../../../core/market/market-status';
import { NotificationService } from '../../../../core/notifications/notification.service';

/**
 * Seller identity and activity summary.
 *
 * The account details come from the validated session (`/api/auth/session`); the activity summary is
 * derived from the seller's own catalogue. Nothing here is editable yet: changing the account is the
 * job of the account-settings page, and a public seller profile would need new schema columns.
 */
@Component({
  selector: 'app-seller-profile',
  imports: [RouterLink],
  templateUrl: './seller-profile.html',
  styleUrl: './seller-profile.css'
})
export class SellerProfileComponent implements OnInit {
  protected readonly i18n = inject(TranslationService);
  protected readonly user = computed(() => this.authSession.user());
  protected readonly lots = signal<readonly MarketLot[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly loadFailed = signal(false);

  private readonly marketApi = inject(MarketApiService);
  private readonly authSession = inject(AuthSessionService);
  private readonly notifications = inject(NotificationService);
  private readonly title = inject(Title);

  protected readonly publishedCount = computed(
    () => this.lots().filter((lot) => lot.statut === 'PUBLIE' || lot.statut === 'RESERVE').length
  );
  protected readonly reservedCount = computed(() => this.lots().filter((lot) => lot.statut === 'RESERVE').length);
  protected readonly availableVolume = computed(
    () => this.lots().filter((lot) => lot.statut === 'PUBLIE' || lot.statut === 'RESERVE')
      .reduce((total, lot) => total + lot.quantiteDisponibleKg, 0)
  );
  protected readonly regions = computed(() => {
    const names = new Set(this.lots().map((lot) => lot.regionNom));
    return [...names].sort((left, right) => left.localeCompare(right));
  });
  protected readonly cocoaTypes = computed(() => {
    const names = new Set(this.lots().map((lot) => lot.typeNom));
    return [...names].sort((left, right) => left.localeCompare(right));
  });

  constructor() {
    effect(() => this.title.setTitle(`${this.i18n.t('common.brandName')} | ${this.i18n.t('market.profile.title')}`));
  }

  ngOnInit(): void {
    this.isLoading.set(true);
    this.marketApi
      .myLots()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.lots.set(response.lots),
        error: () => {
          this.loadFailed.set(true);
          this.notifications.error({ key: 'market.profile.loadError' });
        }
      });
  }

  protected kilograms(value: number): string {
    return formatKilograms(value, this.i18n.language());
  }

  protected price(value: number, devise: string): string {
    return formatPrice(value, devise, this.i18n.language());
  }

  protected lotStatusLabel(status: string): string {
    return lotStatus(status).labelKey;
  }

  protected readonly priceRange = computed(() => {
    const priced = this.lots().filter((lot) => lot.statut === 'PUBLIE' || lot.statut === 'RESERVE');
    if (priced.length === 0) {
      return null;
    }
    const prices = priced.map((lot) => lot.prixKg);
    return {
      min: Math.min(...prices),
      max: Math.max(...prices),
      devise: priced[0].devise
    };
  });
}
