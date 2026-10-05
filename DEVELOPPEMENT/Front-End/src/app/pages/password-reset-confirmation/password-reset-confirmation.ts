import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnDestroy, OnInit, effect, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiErrorResponse, AuthApiService } from '../../core/auth/auth-api.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher';

const LOGIN_REDIRECT_DELAY_SECONDS = 4;

const passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-password-reset-confirmation',
  imports: [ReactiveFormsModule, RouterLink, LanguageSwitcherComponent],
  templateUrl: './password-reset-confirmation.html',
  styleUrl: './password-reset-confirmation.css'
})
export class PasswordResetConfirmationComponent implements OnInit, OnDestroy {
  protected readonly i18n = inject(TranslationService);
  private readonly authApi = inject(AuthApiService);
  private readonly notifications = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<'ready' | 'success' | 'error'>('ready');
  protected readonly errorKey = signal<string | null>(null);
  protected readonly submitted = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly passwordVisible = signal(false);
  protected readonly secondsToRedirect = signal(LOGIN_REDIRECT_DELAY_SECONDS);
  protected readonly resetForm = this.formBuilder.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordsMatch }
  );

  private resetToken = '';
  private redirectTimer: ReturnType<typeof setInterval> | undefined;

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.passwordResetConfirmationTitle')));
    this.destroyRef.onDestroy(() => this.clearRedirectTimer());
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.showError('auth.passwordReset.confirmation.missingToken');
      return;
    }

    this.resetToken = token;
  }

  ngOnDestroy(): void {
    this.clearRedirectTimer();
  }

  protected submit(): void {
    if (this.isSubmitting() || this.state() !== 'ready' || !this.resetToken) {
      return;
    }

    this.submitted.set(true);
    this.errorKey.set(null);
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      this.notifications.warning({ key: 'notifications.forms.invalid' });
      return;
    }

    const formValue = this.resetForm.getRawValue();
    this.isSubmitting.set(true);
    this.authApi.confirmPasswordReset({
      token: this.resetToken,
      password: formValue.password,
      confirmPassword: formValue.confirmPassword
    }).pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.passwordReset.completing' },
        success: { key: 'notifications.passwordReset.completed' },
        error: (error) => ({ key: this.errorTranslationKey(error) })
      }),
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: () => {
        this.state.set('success');
        this.resetToken = '';
        this.passwordVisible.set(false);
        this.resetForm.reset();
        this.resetForm.disable();
        this.startLoginRedirect();
      },
      error: (error: unknown) => this.showError(this.errorTranslationKey(error))
    });
  }

  protected hasError(controlName: 'password' | 'confirmPassword', error: string): boolean {
    const control = this.resetForm.controls[controlName];
    return control.hasError(error) && (control.touched || this.submitted());
  }

  protected hasPasswordMismatch(): boolean {
    const confirmation = this.resetForm.controls.confirmPassword;
    return this.resetForm.hasError('passwordMismatch') && (confirmation.touched || this.submitted());
  }

  protected goToLogin(): void {
    this.clearRedirectTimer();
    void this.router.navigate(['/login']);
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

  private showError(errorKey: string): void {
    this.errorKey.set(errorKey);
    this.state.set('error');
    this.notifications.error({ key: errorKey });
  }

  private errorTranslationKey(error: unknown): string {
    const code = error instanceof HttpErrorResponse && this.isApiError(error.error)
      ? error.error.code
      : undefined;

    switch (code) {
      case 'PASSWORD_RESET_TOKEN_INVALID':
      case 'PASSWORD_RESET_TOKEN_MISSING':
        return 'auth.passwordReset.confirmation.errors.invalid';
      case 'PASSWORD_RESET_TOKEN_EXPIRED':
        return 'auth.passwordReset.confirmation.errors.expired';
      case 'PASSWORD_RESET_TOKEN_ALREADY_USED':
        return 'auth.passwordReset.confirmation.errors.used';
      case 'PASSWORD_RESET_ACCOUNT_UNAVAILABLE':
        return 'auth.passwordReset.confirmation.errors.unavailable';
      case 'PASSWORD_RESET_PASSWORD_INVALID':
        return 'auth.passwordReset.confirmation.errors.passwordInvalid';
      default:
        return 'auth.passwordReset.confirmation.errors.requestFailed';
    }
  }

  private isApiError(value: unknown): value is ApiErrorResponse {
    return typeof value === 'object'
      && value !== null
      && 'code' in value
      && typeof value.code === 'string';
  }
}
