import { TestBed } from '@angular/core/testing';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { NotificationOutletComponent } from './notification-outlet';

describe('NotificationOutletComponent', () => {
  it('renders a localized notification and lets the user dismiss it', async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationOutletComponent]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    const notifications = TestBed.inject(NotificationService);
    i18n.setLanguage('en');
    notifications.clear();
    notifications.success({ key: 'notifications.login.success' }, { durationMs: 0 });

    const fixture = TestBed.createComponent(NotificationOutletComponent);
    fixture.detectChanges();

    const notification = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement;
    expect(notification.textContent).toContain('Success');
    expect(notification.textContent).toContain('You are signed in.');

    const dismiss = notification.querySelector('button') as HTMLButtonElement;
    dismiss.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
  });
});
