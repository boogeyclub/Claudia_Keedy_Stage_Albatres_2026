import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { RegistrationConfirmationComponent } from './registration-confirmation';

function routeWithToken(token?: string): Pick<ActivatedRoute, 'snapshot'> {
  return {
    snapshot: {
      queryParamMap: convertToParamMap(token ? { token } : {})
    }
  } as Pick<ActivatedRoute, 'snapshot'>;
}

describe('RegistrationConfirmationComponent', () => {
  afterEach(() => {
    TestBed.inject(NotificationService).clear();
  });

  it('confirms the token, shows success feedback, and prepares the login redirect', async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationConfirmationComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: routeWithToken('single-use-token') }
      ]
    }).compileComponents();

    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(RegistrationConfirmationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    const request = httpTesting.expectOne((candidate) =>
      candidate.url === '/cacaomarketcm/api/auth/registration/confirm'
      && candidate.params.get('token') === 'single-use-token'
    );
    request.flush({ status: 'CONFIRMED', message: 'Confirmed.' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Your account is confirmed.');
    expect(fixture.nativeElement.textContent).toContain('Redirecting to sign in in 4 seconds.');
    httpTesting.verify();
    fixture.destroy();
  });

  it('shows a localized error and notification when the email link has no token', async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationConfirmationComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: routeWithToken() }
      ]
    }).compileComponents();

    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(RegistrationConfirmationComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('This confirmation link is incomplete.');
    expect(TestBed.inject(NotificationService).notifications()[0]).toEqual(expect.objectContaining({
      kind: 'error',
      key: 'notifications.confirmation.missingToken'
    }));
    fixture.destroy();
  });
});
