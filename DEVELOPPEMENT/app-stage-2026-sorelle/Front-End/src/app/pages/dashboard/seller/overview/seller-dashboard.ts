import { Component, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-seller-dashboard',
  imports: [RouterLink],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.css'
})
export class SellerDashboardComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly title = inject(Title);

  protected readonly workspaceCards = [
    { index: '01', icon: 'lot', titleKey: 'dashboard.seller.prepareCard.title', descriptionKey: 'dashboard.seller.prepareCard.description' },
    { index: '02', icon: 'profile', titleKey: 'dashboard.seller.profileCard.title', descriptionKey: 'dashboard.seller.profileCard.description' },
    { index: '03', icon: 'conversation', titleKey: 'dashboard.seller.conversationsCard.title', descriptionKey: 'dashboard.seller.conversationsCard.description' }
  ] as const;

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.sellerDashboardTitle')));
  }
}
