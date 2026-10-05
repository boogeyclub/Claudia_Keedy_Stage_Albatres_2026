import { Component, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-client-dashboard',
  imports: [RouterLink],
  templateUrl: './client-dashboard.html',
  styleUrl: './client-dashboard.css'
})
export class ClientDashboardComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly title = inject(Title);

  protected readonly workspaceCards = [
    { icon: 'search', titleKey: 'dashboard.user.discoverCard.title', descriptionKey: 'dashboard.user.discoverCard.description' },
    { icon: 'sliders', titleKey: 'dashboard.user.preferencesCard.title', descriptionKey: 'dashboard.user.preferencesCard.description' },
    { icon: 'message', titleKey: 'dashboard.user.conversationsCard.title', descriptionKey: 'dashboard.user.conversationsCard.description' }
  ] as const;

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.userDashboardTitle')));
  }
}
