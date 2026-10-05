import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthApiService, RegistrationPayload } from './auth-api.service';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AuthApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('posts the registration form to the backend registration route', () => {
    const payload: RegistrationPayload = {
      role: 'VENDEUR',
      prenom: 'Amina',
      nom: 'Ngono',
      email: 'amina@example.com',
      login: 'amina-cocoa',
      password: 'secure-passphrase',
      confirmPassword: 'secure-passphrase',
      acceptTerms: true,
      language: 'fr'
    };

    service.register(payload).subscribe((response) => {
      expect(response.email).toBe(payload.email);
    });

    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/registration');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual(payload);
    request.flush({ email: payload.email, expiresAt: '2026-09-22T18:00:00Z' });
  });

  it('posts the enterprise buyer profile with the registration request', () => {
    const payload: RegistrationPayload = {
      role: 'CLIENT',
      clientProfileType: 'ENTREPRISE',
      raisonSociale: 'Cacao Source Cameroun SARL',
      niu: 'M012345678901A',
      rccm: 'RC/YAO/2026/B/123',
      prenom: 'Amina',
      nom: 'Ngono',
      email: 'amina@example.com',
      login: 'amina-cocoa',
      password: 'secure-passphrase',
      confirmPassword: 'secure-passphrase',
      acceptTerms: true,
      language: 'fr'
    };

    service.register(payload).subscribe();

    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/registration');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ email: payload.email, expiresAt: '2026-09-22T18:00:00Z' });
  });

  it('sends an email confirmation token to the backend confirmation route', () => {
    service.confirmRegistration('single-use-token').subscribe((response) => {
      expect(response.status).toBe('CONFIRMED');
    });

    const request = httpTesting.expectOne((candidate) =>
      candidate.url === '/cacaomarketcm/api/auth/registration/confirm'
      && candidate.params.get('token') === 'single-use-token'
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ status: 'CONFIRMED', message: 'Confirmed.' });
  });

  it('requests and confirms a password reset through the direct API', () => {
    const requestPayload = { email: 'buyer@example.com', language: 'fr' as const };
    service.requestPasswordReset(requestPayload).subscribe((response) => {
      expect(response.message).toContain('confirmed');
    });

    const requestReset = httpTesting.expectOne('/cacaomarketcm/api/auth/password-reset/request');
    expect(requestReset.request.method).toBe('POST');
    expect(requestReset.request.withCredentials).toBe(true);
    expect(requestReset.request.body).toEqual(requestPayload);
    requestReset.flush({ message: 'If a confirmed CacaoMarketCM account uses this email address, a password reset link has been sent.' });

    const confirmationPayload = {
      token: 'single-use-reset-token',
      password: 'new-secure-password',
      confirmPassword: 'new-secure-password'
    };
    service.confirmPasswordReset(confirmationPayload).subscribe((response) => {
      expect(response.status).toBe('RESET');
    });

    const confirmReset = httpTesting.expectOne('/cacaomarketcm/api/auth/password-reset/confirm');
    expect(confirmReset.request.method).toBe('POST');
    expect(confirmReset.request.withCredentials).toBe(true);
    expect(confirmReset.request.body).toEqual(confirmationPayload);
    confirmReset.flush({ status: 'RESET', message: 'Password reset.' });
  });

  it('posts login credentials to the backend login route', () => {
    service.login({ identity: 'amina-cocoa', password: 'secure-passphrase', rememberMe: true }).subscribe((user) => {
      expect(user.role).toBe('VENDEUR');
    });

    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    request.flush({
      id: 1,
      email: 'amina@example.com',
      login: 'amina-cocoa',
      prenom: 'Amina',
      nom: 'Ngono',
      role: 'VENDEUR'
    });
  });

  it('restores the authenticated profile from the protected browser-session route', () => {
    service.currentSession().subscribe((user) => {
      expect(user.role).toBe('CLIENT');
    });

    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/session');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush({
      id: 7,
      email: 'buyer@example.com',
      login: 'buyer-team',
      prenom: 'Noah',
      nom: 'Buyer',
      role: 'CLIENT'
    });
  });

  it('lists and disconnects only the signed-in user browser sessions', () => {
    service.browserSessions().subscribe((sessions) => {
      expect(sessions[0].current).toBe(true);
    });

    const listRequest = httpTesting.expectOne('/cacaomarketcm/api/auth/sessions');
    expect(listRequest.request.method).toBe('GET');
    expect(listRequest.request.withCredentials).toBe(true);
    listRequest.flush([{
      id: 31,
      browserLabel: 'Google Chrome on Windows',
      rememberMe: false,
      createdAt: '2026-09-23T12:00:00Z',
      lastSeenAt: '2026-09-23T12:01:00Z',
      expiresAt: '2026-09-23T12:31:00Z',
      current: true
    }]);

    service.disconnectBrowserSession(31).subscribe();
    const disconnectRequest = httpTesting.expectOne('/cacaomarketcm/api/auth/sessions/31');
    expect(disconnectRequest.request.method).toBe('DELETE');
    expect(disconnectRequest.request.withCredentials).toBe(true);
    disconnectRequest.flush(null);
  });

  it('posts to the configured direct API when logging out', () => {
    service.logout().subscribe();

    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/logout');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    request.flush(null);
  });
});
