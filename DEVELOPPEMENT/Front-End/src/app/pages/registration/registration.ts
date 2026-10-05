import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  ApiErrorResponse,
  AuthApiService,
  ClientProfileType,
  RegistrableUserRole
} from '../../core/auth/auth-api.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher';

const passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  return password === confirmPassword ? null : { passwordMismatch: true };
};

type RegistrationControlName =
  | 'role'
  | 'clientProfileType'
  | 'raisonSociale'
  | 'niu'
  | 'rccm'
  | 'prenom'
  | 'nom'
  | 'email'
  | 'login'
  | 'password'
  | 'confirmPassword'
  | 'acceptTerms';

@Component({
  selector: 'app-registration',
  imports: [ReactiveFormsModule, RouterLink, LanguageSwitcherComponent],
  templateUrl: './registration.html',
  styleUrl: './registration.css'
})
export class RegistrationComponent {
  protected readonly i18n = inject(TranslationService);
  private readonly authApi = inject(AuthApiService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly title = inject(Title);

  protected readonly passwordVisible = signal(false);
  protected readonly submitted = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly registrationState = signal<'idle' | 'success' | 'error'>('idle');
  protected readonly registrationEmail = signal('');
  protected readonly registrationErrorKey = signal<string | null>(null);
  protected readonly registrationForm = this.formBuilder.nonNullable.group(
    {
      role: ['VENDEUR', [Validators.required]],
      clientProfileType: [''],
      raisonSociale: [''],
      niu: [''],
      rccm: [''],
      prenom: ['', [Validators.required]],
      nom: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      login: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]]
    },
    { validators: passwordsMatch }
  );

  constructor() {
    effect(() => this.title.setTitle(this.i18n.t('meta.registrationTitle')));
    this.registrationForm.controls.role.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.updateClientProfileValidation());
    this.registrationForm.controls.clientProfileType.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.updateClientProfileValidation());
    this.updateClientProfileValidation();
  }

  protected submit(): void {
    if (this.isSubmitting() || this.registrationState() === 'success') {
      return;
    }

    this.submitted.set(true);
    this.registrationState.set('idle');
    this.registrationErrorKey.set(null);

    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      this.notifications.warning({ key: 'notifications.forms.invalid' });
      return;
    }

    const formValue = this.registrationForm.getRawValue();
    const isClient = formValue.role === 'CLIENT';
    const isEnterprise = isClient && formValue.clientProfileType === 'ENTREPRISE';
    this.isSubmitting.set(true);

    this.authApi.register({
      role: formValue.role as RegistrableUserRole,
      clientProfileType: isClient ? (formValue.clientProfileType as ClientProfileType) : undefined,
      raisonSociale: isEnterprise ? formValue.raisonSociale.trim() : undefined,
      niu: isEnterprise ? formValue.niu.trim() : undefined,
      rccm: isEnterprise ? formValue.rccm.trim() : undefined,
      prenom: formValue.prenom.trim(),
      nom: formValue.nom.trim(),
      email: formValue.email.trim(),
      login: formValue.login.trim(),
      password: formValue.password,
      confirmPassword: formValue.confirmPassword,
      acceptTerms: formValue.acceptTerms,
      language: this.i18n.language()
    }).pipe(
      this.notifications.trackApiCall({
        start: { key: 'notifications.registration.starting' },
        success: { key: 'notifications.registration.success' },
        error: (error) => ({ key: this.errorTranslationKey(error) })
      }),
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: (response) => {
        this.registrationEmail.set(response.email);
        this.registrationState.set('success');
        this.passwordVisible.set(false);
        this.registrationForm.reset();
        this.registrationForm.disable();
      },
      error: (error: unknown) => {
        this.registrationErrorKey.set(this.errorTranslationKey(error));
        this.registrationState.set('error');
      }
    });
  }

  protected hasError(controlName: RegistrationControlName, error: string): boolean {
    const control = this.registrationForm.controls[controlName];
    return control.hasError(error) && (control.touched || this.submitted());
  }

  protected hasPasswordMismatch(): boolean {
    const confirmation = this.registrationForm.controls.confirmPassword;
    return this.registrationForm.hasError('passwordMismatch') && (confirmation.touched || this.submitted());
  }

  protected isClientRegistration(): boolean {
    return this.registrationForm.controls.role.value === 'CLIENT';
  }

  protected isEnterpriseRegistration(): boolean {
    return this.isClientRegistration() && this.registrationForm.controls.clientProfileType.value === 'ENTREPRISE';
  }

  protected firstNameLabelKey(): string {
    return this.isEnterpriseRegistration()
      ? 'auth.registration.representativeFirstNameLabel'
      : 'auth.registration.firstNameLabel';
  }

  protected firstNamePlaceholderKey(): string {
    return this.isEnterpriseRegistration()
      ? 'auth.registration.representativeFirstNamePlaceholder'
      : 'auth.registration.firstNamePlaceholder';
  }

  protected lastNameLabelKey(): string {
    return this.isEnterpriseRegistration()
      ? 'auth.registration.representativeLastNameLabel'
      : 'auth.registration.lastNameLabel';
  }

  protected lastNamePlaceholderKey(): string {
    return this.isEnterpriseRegistration()
      ? 'auth.registration.representativeLastNamePlaceholder'
      : 'auth.registration.lastNamePlaceholder';
  }

  private updateClientProfileValidation(): void {
    const controls = this.registrationForm.controls;
    const isClient = controls.role.value === 'CLIENT';
    const isEnterprise = isClient && controls.clientProfileType.value === 'ENTREPRISE';

    controls.clientProfileType.setValidators(isClient ? [Validators.required] : []);
    controls.clientProfileType.updateValueAndValidity({ emitEvent: false });

    controls.raisonSociale.setValidators(isEnterprise ? [Validators.required, Validators.maxLength(150)] : []);
    controls.niu.setValidators(isEnterprise ? [Validators.required, Validators.maxLength(50)] : []);
    controls.rccm.setValidators(isEnterprise ? [Validators.required, Validators.maxLength(50)] : []);
    for (const control of [controls.raisonSociale, controls.niu, controls.rccm]) {
      control.updateValueAndValidity({ emitEvent: false });
    }

    if (!isClient) {
      controls.clientProfileType.setValue('', { emitEvent: false });
    }
    if (!isEnterprise) {
      controls.raisonSociale.setValue('', { emitEvent: false });
      controls.niu.setValue('', { emitEvent: false });
      controls.rccm.setValue('', { emitEvent: false });
    }
  }

  private errorTranslationKey(error: unknown): string {
    const code = error instanceof HttpErrorResponse && this.isApiError(error.error)
      ? error.error.code
      : undefined;

    switch (code) {
      case 'REGISTRATION_IDENTITY_ALREADY_EXISTS':
        return 'auth.registration.errors.identityExists';
      case 'REGISTRATION_ENTERPRISE_IDENTIFIER_ALREADY_EXISTS':
        return 'auth.registration.errors.enterpriseIdentifierExists';
      case 'REGISTRATION_IDENTITY_OR_ENTERPRISE_IDENTIFIER_EXISTS':
        return 'auth.registration.errors.identityOrEnterpriseIdentifierExists';
      case 'REGISTRATION_CLIENT_PROFILE_INVALID':
      case 'REGISTRATION_CLIENT_PROFILE_FORBIDDEN':
      case 'REGISTRATION_CLIENT_PROFILE_DETAILS_FORBIDDEN':
      case 'REGISTRATION_ENTERPRISE_DETAILS_INVALID':
        return 'auth.registration.errors.clientProfileInvalid';
      case 'REGISTRATION_MAIL_DELIVERY_UNAVAILABLE':
        return 'auth.registration.errors.deliveryUnavailable';
      default:
        return 'auth.registration.errors.requestFailed';
    }
  }

  private isApiError(value: unknown): value is ApiErrorResponse {
    return typeof value === 'object'
      && value !== null
      && 'code' in value
      && typeof value.code === 'string';
  }
}
