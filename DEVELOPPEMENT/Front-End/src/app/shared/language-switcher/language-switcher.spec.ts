import { TestBed } from '@angular/core/testing';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { LanguageSwitcherComponent } from './language-switcher';

describe('LanguageSwitcherComponent', () => {
  it('localizes language names and accessibility labels', async () => {
    await TestBed.configureTestingModule({
      imports: [LanguageSwitcherComponent]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    const notifications = TestBed.inject(NotificationService);
    notifications.clear();
    const fixture = TestBed.createComponent(LanguageSwitcherComponent);

    i18n.setLanguage('en');
    fixture.detectChanges();

    let buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    expect(buttons.map((button) => button.textContent?.trim())).toEqual(['EN', 'FR']);
    expect(buttons.map((button) => button.getAttribute('aria-label'))).toEqual([
      'Switch to English',
      'Switch to French'
    ]);

    buttons[1].click();
    fixture.detectChanges();

    buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    expect(buttons.map((button) => button.getAttribute('aria-label'))).toEqual([
      'Passer en anglais',
      'Passer en français'
    ]);
    expect(notifications.notifications()[0]).toEqual(expect.objectContaining({
      kind: 'info',
      key: 'notifications.language.changed',
      params: { language: 'français' }
    }));
    notifications.clear();
  });
});
