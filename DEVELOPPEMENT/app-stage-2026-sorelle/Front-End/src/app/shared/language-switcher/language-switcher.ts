import { Component, inject } from '@angular/core';
import { TranslationService, SupportedLanguage } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-language-switcher',
  template: `
    <div
      class="inline-flex rounded-full border border-cacao-gold/40 bg-cacao-pale/80 p-1"
      role="group"
      [attr.aria-label]="i18n.t('common.languageSelector')"
    >
      @for (language of i18n.languages; track language.code) {
        <button
          class="rounded-full px-2.5 py-1 text-[0.68rem] font-extrabold tracking-[0.08em] transition sm:px-3"
          type="button"
          [class.bg-white]="i18n.language() === language.code"
          [class.text-cacao-ink]="i18n.language() === language.code"
          [class.shadow-sm]="i18n.language() === language.code"
          [class.text-cacao-brown]="i18n.language() !== language.code"
          [attr.aria-pressed]="i18n.language() === language.code"
          [attr.aria-label]="i18n.t('common.switchToLanguage', { language: i18n.t(language.labelKey) })"
          (click)="switchLanguage(language.code, language.labelKey)"
        >
          {{ i18n.t(language.shortLabelKey) }}
        </button>
      }
    </div>
  `
})
export class LanguageSwitcherComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly notifications = inject(NotificationService);

  protected switchLanguage(language: SupportedLanguage, labelKey: string): void {
    if (this.i18n.language() === language) {
      return;
    }

    this.i18n.setLanguage(language);
    this.notifications.info({
      key: 'notifications.language.changed',
      params: { language: this.i18n.t(labelKey) }
    });
  }
}
