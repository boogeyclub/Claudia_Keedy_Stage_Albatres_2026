import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { PasswordResetConfirmationComponent } from './password-reset-confirmation';

function routeWithToken(token?: string): Pick<ActivatedRoute, 'snapshot'> {
  return {
    snapshot: {
      queryParamMap: convertToParamMap(token ? { token } : {})
    }
  } as Pick<ActivatedRoute, 'snapshot'>;
}

describe('PasswordResetConfirmationComponent', () => {
  afterEach(() => {
    TestBed.inject(NotificationService).clear();
  });

  it('posts a matching new password without rendering the reset token', async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordResetConfirmationComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: routeWithToken('single-use-reset-token') }
      ]
    }).compileComponents();

    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(PasswordResetConfirmationComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      resetForm: { patchValue(value: { password: string; confirmPassword: string }): void };
      submit(): void;
    };
    component.resetForm.patchValue({ password: 'new-secure-password', confirmPassword: 'new-secure-password' });
    component.submit();

    const httpTesting = TestBed.inject(HttpTestingController);
    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/password-reset/confirm');
    expect(request.request.body).toEqual({
      token: 'single-use-reset-token',
      password: 'new-secure-password',
      confirmPassword: 'new-secure-password'
    });
    request.flush({ status: 'RESET', message: 'Password reset.' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Your password was reset.');
    expect(fixture.nativeElement.textContent).not.toContain('single-use-reset-token');
    httpTesting.verify();
    fixture.destroy();
  });

  it('shows a clear error when the reset link is incomplete', async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordResetConfirmationComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: routeWithToken() }
      ]
    }).compileComponents();

    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(PasswordResetConfirmationComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('This reset link is incomplete.');
    expect(TestBed.inject(NotificationService).notifications()[0]).toEqual(expect.objectContaining({
      kind: 'error',
      key: 'auth.passwordReset.confirmation.missingToken'
    }));
    fixture.destroy();
  });
});
