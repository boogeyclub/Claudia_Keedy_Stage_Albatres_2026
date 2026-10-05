import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { PasswordResetRequestComponent } from './password-reset-request';

describe('PasswordResetRequestComponent', () => {
  it('requires a valid email before a reset link can be requested', async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordResetRequestComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    const fixture = TestBed.createComponent(PasswordResetRequestComponent);
    const component = fixture.componentInstance as unknown as {
      resetRequestForm: { patchValue(value: { email: string }): void; valid: boolean };
    };

    expect(component.resetRequestForm.valid).toBe(false);
    component.resetRequestForm.patchValue({ email: 'buyer@example.com' });
    expect(component.resetRequestForm.valid).toBe(true);
  });

  it('renders the French password-recovery page and localized title', async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordResetRequestComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    TestBed.inject(TranslationService).setLanguage('fr');
    const fixture = TestBed.createComponent(PasswordResetRequestComponent);
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const email = nativeElement.querySelector('#password-reset-email') as HTMLInputElement;
    expect(nativeElement.textContent).toContain('Réinitialisez votre mot de passe en toute sécurité.');
    expect(email.placeholder).toBe('vous@exemple.com');
    expect(TestBed.inject(Title).getTitle()).toBe('CacaoMarketCM | Réinitialiser votre mot de passe');
  });
});
