import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnDestroy, OnInit, effect, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of, throwError } from 'rxjs';
import { ApiErrorResponse, AuthApiService, ConfirmationResponse } from '../../core/auth/auth-api.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher';

type ConfirmationState = 'confirming' | 'success' | 'error';

const LOGIN_REDIRECT_DELAY_SECONDS = 4;

@Component({
  selector: 'app-registration-confirmation',
  imports: [RouterLink, LanguageSwitcherComponent],
  templateUrl: './registration-confirmation.html',
  styleUrl: './registration-confirmation.css'
})
export class RegistrationConfirmationComponent implements OnInit, OnDestroy {
  protected readonly i18n = inject(TranslationService);
  private readonly authApi = inject(AuthApiService);
  private readonly notifications = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<ConfirmationState>('confirming');
  protected readonly errorKey = signal<string | null>(null);
  protected readonly secondsToRedirect = signal(LOGIN_REDIRECT_DELAY_SECONDS);

  private redirectTimer: ReturnType<typeof setInterval> | undefined;

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.confirmationTitle')));
    this.destroyRef.onDestroy(() => this.clearRedirectTimer());
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.showError('auth.confirmation.missingToken', 'notifications.confirmation.missingToken');
      return;
    }

    this.authApi.confirmRegistration(token).pipe(
      catchError((error: unknown) => this.errorCode(error) === 'REGISTRATION_ALREADY_CONFIRMED'
        ? of<ConfirmationResponse>({ status: 'ALREADY_CONFIRMED', message: '' })
        : throwError(() => error)
      ),
      this.notifications.trackApiCall({
        start: { key: 'notifications.confirmation.starting' },
        success: (response) => ({
          key: response.status === 'ALREADY_CONFIRMED'
            ? 'notifications.confirmation.alreadyConfirmed'
            : 'notifications.confirmation.success'
        }),
        error: (error) => ({ key: this.errorTranslationKey(error) })
      })
    ).subscribe({
      next: () => {
        this.state.set('success');
        this.startLoginRedirect();
      },
      error: (error: unknown) => {
        this.showError(this.errorTranslationKey(error));
      }
    });
  }

  ngOnDestroy(): void {
    this.clearRedirectTimer();
  }

  protected goToLogin(): void {
    this.clearRedirectTimer();
    void this.router.navigate(['/login']);
  }

  private showError(errorKey: string, notificationKey = errorKey): void {
    this.errorKey.set(errorKey);
    this.state.set('error');
    this.notifications.error({ key: notificationKey });
  }

  private startLoginRedirect(): void {
    let secondsRemaining = LOGIN_REDIRECT_DELAY_SECONDS;
    this.secondsToRedirect.set(secondsRemaining);
    this.clearRedirectTimer();

    this.redirectTimer = setInterval(() => {
      secondsRemaining -= 1;
      this.secondsToRedirect.set(Math.max(secondsRemaining, 0));

      if (secondsRemaining <= 0) {
        this.goToLogin();
      }
    }, 1000);
  }

  private clearRedirectTimer(): void {
    if (this.redirectTimer !== undefined) {
      clearInterval(this.redirectTimer);
      this.redirectTimer = undefined;
    }
  }

  private errorTranslationKey(error: unknown): string {
    switch (this.errorCode(error)) {
      case 'REGISTRATION_CONFIRMATION_TOKEN_INVALID':
      case 'REGISTRATION_CONFIRMATION_TOKEN_MISSING':
        return 'auth.confirmation.errors.invalid';
      case 'REGISTRATION_CONFIRMATION_EXPIRED':
        return 'auth.confirmation.errors.expired';
      case 'REGISTRATION_CONFIRMATION_UNAVAILABLE':
        return 'auth.confirmation.errors.unavailable';
      default:
        return 'auth.confirmation.errors.requestFailed';
    }
  }

  private errorCode(error: unknown): string | undefined {
    return error instanceof HttpErrorResponse && this.isApiError(error.error)
      ? error.error.code
      : undefined;
  }

  private isApiError(value: unknown): value is ApiErrorResponse {
    return typeof value === 'object'
      && value !== null
      && 'code' in value
      && typeof value.code === 'string';
  }
}
