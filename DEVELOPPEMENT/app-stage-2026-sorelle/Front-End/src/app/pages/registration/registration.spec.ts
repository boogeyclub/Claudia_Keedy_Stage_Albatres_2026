import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { RegistrationComponent } from './registration';

describe('RegistrationComponent', () => {
  it('requires matching passwords before registration is valid', async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    i18n.setLanguage('en');

    const fixture = TestBed.createComponent(RegistrationComponent);
    const component = fixture.componentInstance as unknown as {
      registrationForm: {
        patchValue(value: Record<string, string | boolean>): void;
        hasError(error: string): boolean;
        valid: boolean;
      };
    };

    component.registrationForm.patchValue({
      role: 'VENDEUR',
      prenom: 'Amina',
      nom: 'Ngono',
      email: 'amina@example.com',
      login: 'amina-cocoa',
      password: 'secure-passphrase',
      confirmPassword: 'different-passphrase',
      acceptTerms: true
    });

    expect(component.registrationForm.hasError('passwordMismatch')).toBe(true);
    expect(component.registrationForm.valid).toBe(false);

    component.registrationForm.patchValue({ confirmPassword: 'secure-passphrase' });

    expect(component.registrationForm.valid).toBe(true);
  });

  it('requires a buyer legal profile and enterprise identifiers for CLIENT registration', async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    const fixture = TestBed.createComponent(RegistrationComponent);
    const component = fixture.componentInstance as unknown as {
      registrationForm: {
        patchValue(value: Record<string, string | boolean>): void;
        valid: boolean;
      };
    };

    component.registrationForm.patchValue({
      role: 'CLIENT',
      prenom: 'Amina',
      nom: 'Ngono',
      email: 'amina@example.com',
      login: 'amina-cocoa',
      password: 'secure-passphrase',
      confirmPassword: 'secure-passphrase',
      acceptTerms: true
    });

    expect(component.registrationForm.valid).toBe(false);

    component.registrationForm.patchValue({ clientProfileType: 'PARTICULIER' });
    expect(component.registrationForm.valid).toBe(true);

    component.registrationForm.patchValue({ clientProfileType: 'ENTREPRISE' });
    expect(component.registrationForm.valid).toBe(false);

    component.registrationForm.patchValue({
      raisonSociale: 'Cacao Source Cameroun SARL',
      niu: 'M012345678901A',
      rccm: 'RC/YAO/2026/B/123'
    });
    expect(component.registrationForm.valid).toBe(true);
  });

  it('renders the registration content, validation text, and document title in French', async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    i18n.setLanguage('fr');

    const fixture = TestBed.createComponent(RegistrationComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      submit(): void;
      registrationForm: { patchValue(value: Record<string, string>): void };
    };
    component.registrationForm.patchValue({ role: 'CLIENT', clientProfileType: 'ENTREPRISE' });
    component.submit();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const email = nativeElement.querySelector('#registration-email') as HTMLInputElement;
    const login = nativeElement.querySelector('#registration-login') as HTMLInputElement;

    expect(nativeElement.textContent).toContain('Préparez votre prochaine transaction cacao avec clarté.');
    expect(nativeElement.textContent).toContain('Comment allez-vous utiliser CacaoMarketCM ?');
    expect(nativeElement.textContent).toContain('Sous quel statut recherchez-vous du cacao ?');
    expect(nativeElement.textContent).toContain('Identification de l’entreprise');
    expect(nativeElement.textContent).toContain('Confirmez votre mot de passe.');
    expect(email.placeholder).toBe('vous@exemple.com');
    expect(login.placeholder).toBe('Votre identifiant préféré');
    expect(TestBed.inject(Title).getTitle()).toBe('CacaoMarketCM | Créer votre compte');
  });
});
