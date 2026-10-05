import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiErrorResponse, AuthApiService } from '../../core/auth/auth-api.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher';

@Component({
  selector: 'app-password-reset-request',
  imports: [ReactiveFormsModule, RouterLink, LanguageSwitcherComponent],
  templateUrl: './password-reset-request.html',
  styleUrl: './password-reset-request.css'
})
export class PasswordResetRequestComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly authApi = inject(AuthApiService);
  private readonly notifications = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly title = inject(Title);

  protected readonly submitted = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly state = signal<'idle' | 'success' | 'error'>('idle');
  protected readonly errorKey = signal<string | null>(null);
  protected readonly resetRequestForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.passwordResetRequestTitle')));
  }

  protected submit(): void {
    if (this.isSubmitting() || this.state() === 'success') {
      return;
    }

    this.submitted.set(true);
    this.state.set('idle');
    this.errorKey.set(null);
    if (this.resetRequestForm.invalid) {
      this.resetRequestForm.markAllAsTouched();
      this.notifications.warning({ key: 'notifications.forms.invalid' });
      return;
    }

    this.isSubmitting.set(true);
    const email = this.resetRequestForm.controls.email.value.trim();
    this.authApi.requestPasswordReset({ email, language: this.i18n.language() }).pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.passwordReset.requesting' },
        success: { key: 'notifications.passwordReset.requestAccepted' },
        error: (error) => ({ key: this.errorTranslationKey(error) })
      }),
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: () => {
        this.state.set('success');
        this.resetRequestForm.disable();
      },
      error: (error: unknown) => {
        this.errorKey.set(this.errorTranslationKey(error));
        this.state.set('error');
      }
    });
  }

  protected hasError(error: 'required' | 'email'): boolean {
    const control = this.resetRequestForm.controls.email;
    return control.hasError(error) && (control.touched || this.submitted());
  }

  private errorTranslationKey(error: unknown): string {
    const code = error instanceof HttpErrorResponse && this.isApiError(error.error)
      ? error.error.code
      : undefined;

    return code === 'PASSWORD_RESET_MAIL_DELIVERY_UNAVAILABLE'
      ? 'auth.passwordReset.request.errors.deliveryUnavailable'
      : 'auth.passwordReset.request.errors.requestFailed';
  }

  private isApiError(value: unknown): value is ApiErrorResponse {
    return typeof value === 'object'
      && value !== null
      && 'code' in value
      && typeof value.code === 'string';
  }
}
