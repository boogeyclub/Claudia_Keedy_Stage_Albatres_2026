import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { LoginComponent } from './login';

describe('LoginComponent', () => {
  it('requires a valid identity and password before the form is valid', async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    i18n.setLanguage('en');

    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance as unknown as { loginForm: { patchValue(value: { identity: string; password: string }): void; valid: boolean } };

    expect(component.loginForm.valid).toBe(false);

    component.loginForm.patchValue({
      identity: 'cocoa-buyer',
      password: 'secure-passphrase'
    });

    expect(component.loginForm.valid).toBe(true);
  });

  it('renders the login content and document title in French', async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    const i18n = TestBed.inject(TranslationService);
    i18n.setLanguage('fr');

    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const identity = nativeElement.querySelector('#login-identity') as HTMLInputElement;
    const password = nativeElement.querySelector('#login-password') as HTMLInputElement;

    expect(nativeElement.textContent).toContain('Connectez-vous pour faire avancer votre commerce du cacao.');
    expect(nativeElement.textContent).toContain('Garder cette session active plus longtemps');
    expect(nativeElement.textContent).toContain('Mot de passe oublié ?');
    expect((nativeElement.querySelector('a[href$="password-reset"]') as HTMLAnchorElement).getAttribute('href')).toContain('password-reset');
    expect(identity.placeholder).toBe('vous@exemple.com ou votre identifiant');
    expect(password.placeholder).toBe('Saisissez votre mot de passe');
    expect(TestBed.inject(Title).getTitle()).toBe('CacaoMarketCM | Connexion');
  });
});
