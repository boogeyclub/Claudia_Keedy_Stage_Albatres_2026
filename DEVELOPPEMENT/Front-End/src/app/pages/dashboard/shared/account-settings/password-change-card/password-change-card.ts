import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthApiService } from '../../../../../core/auth/auth-api.service';
import { TranslationService } from '../../../../../core/i18n/translation.service';
import { NotificationService } from '../../../../../core/notifications/notification.service';

/**
 * Self-service password change for the signed-in account.
 *
 * The form is deliberately filled by hand every time (no autofill) and cleared on success: the
 * browser must never keep a typed password in the page after the change.
 */
@Component({
  selector: 'app-password-change-card',
  imports: [ReactiveFormsModule],
  templateUrl: './password-change-card.html',
  styleUrl: './password-change-card.css'
})
export class PasswordChangeCardComponent {
  protected readonly i18n = inject(TranslationService);
  protected readonly isSaving = signal(false);
  protected readonly isPasswordVisible = signal(false);
  protected passwordForm: UntypedFormGroup;

  private readonly authApi = inject(AuthApiService);
  private readonly notifications = inject(NotificationService);
  private readonly formBuilder = inject(UntypedFormBuilder);

  constructor() {
    this.passwordForm = this.formBuilder.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: [matchingPasswords] });
  }

  protected togglePasswordVisibility(): void {
    this.isPasswordVisible.update((visible) => !visible);
  }

  protected submit(): void {
    if (this.isSaving()) {
      return;
    }
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      this.notifications.warning({ key: 'dashboard.account.password.invalid' });
      return;
    }

    const values = this.passwordForm.value as Record<string, string>;
    this.isSaving.set(true);
    this.authApi.changePassword({
      currentPassword: values['currentPassword'],
      newPassword: values['newPassword'],
      confirmPassword: values['confirmPassword']
    }).pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.passwordForm.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
        this.notifications.success({ key: 'notifications.passwordChange.success' });
      },
      error: () => this.notifications.error({ key: 'notifications.passwordChange.failed' })
    });
  }
}

function matchingPasswords(group: UntypedFormGroup): { passwordsMismatch: boolean } | null {
  const newPassword = group.get('newPassword')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return newPassword && confirmPassword && newPassword !== confirmPassword
    ? { passwordsMismatch: true }
    : null;
}
