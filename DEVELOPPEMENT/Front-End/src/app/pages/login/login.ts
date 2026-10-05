import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiErrorResponse, AuthApiService } from '../../core/auth/auth-api.service';
import { dashboardPathForRole } from '../../core/auth/auth-role';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, LanguageSwitcherComponent],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly authApi = inject(AuthApiService);
  private readonly authSession = inject(AuthSessionService);
  private readonly notifications = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly title = inject(Title);
  private readonly router = inject(Router);

  protected readonly passwordVisible = signal(false);
  protected readonly submitted = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly loginState = signal<'idle' | 'success' | 'error'>('idle');
  protected readonly authenticatedName = signal('');
  protected readonly loginErrorKey = signal<string | null>(null);
  protected readonly loginForm = this.formBuilder.nonNullable.group({
    identity: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    rememberMe: [false]
  });

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.loginTitle')));
  }

  protected submit(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.submitted.set(true);
    this.loginState.set('idle');
    this.loginErrorKey.set(null);

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.notifications.warning({ key: 'notifications.forms.invalid' });
      return;
    }

    const formValue = this.loginForm.getRawValue();
    this.isSubmitting.set(true);

    this.authApi.login({
      identity: formValue.identity.trim(),
      password: formValue.password,
      rememberMe: formValue.rememberMe
    }).pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.login.starting' },
        success: { key: 'notifications.login.success' },
        error: (error) => ({ key: this.errorTranslationKey(error) })
      }),
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: (user) => {
        this.authSession.setAuthenticatedUser(user);
        this.authenticatedName.set(user.prenom);
        this.loginState.set('success');
        this.passwordVisible.set(false);
        this.loginForm.controls.password.reset();
        void this.router.navigateByUrl(dashboardPathForRole(user.role));
      },
      error: (error: unknown) => {
        this.loginErrorKey.set(this.errorTranslationKey(error));
        this.loginState.set('error');
      }
    });
  }

  protected hasError(controlName: 'identity' | 'password', error: string): boolean {
    const control = this.loginForm.controls[controlName];
    return control.hasError(error) && (control.touched || this.submitted());
  }

  private errorTranslationKey(error: unknown): string {
    const code = error instanceof HttpErrorResponse && this.isApiError(error.error)
      ? error.error.code
      : undefined;

    switch (code) {
      case 'INVALID_CREDENTIALS':
        return 'auth.login.errors.invalidCredentials';
      case 'REGISTRATION_PENDING_CONFIRMATION':
        return 'auth.login.errors.pendingConfirmation';
      default:
        return 'auth.login.errors.requestFailed';
    }
  }

  private isApiError(value: unknown): value is ApiErrorResponse {
    return typeof value === 'object'
      && value !== null
      && 'code' in value
      && typeof value.code === 'string';
  }
}
