import { Component, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { ADMIN_TABLE_CATALOG } from '../../../../core/admin/admin-table-catalog';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css'
})
export class AdminDashboardComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly title = inject(Title);

  protected readonly focusCards = [
    { icon: 'shield', titleKey: 'dashboard.admin.accessCard.title', descriptionKey: 'dashboard.admin.accessCard.description' },
    { icon: 'monitor', titleKey: 'dashboard.admin.sessionsCard.title', descriptionKey: 'dashboard.admin.sessionsCard.description' },
    { icon: 'chart', titleKey: 'dashboard.admin.marketCard.title', descriptionKey: 'dashboard.admin.marketCard.description' }
  ] as const;
  protected readonly tableCards = ADMIN_TABLE_CATALOG;

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.administratorDashboardTitle')));
  }
}
