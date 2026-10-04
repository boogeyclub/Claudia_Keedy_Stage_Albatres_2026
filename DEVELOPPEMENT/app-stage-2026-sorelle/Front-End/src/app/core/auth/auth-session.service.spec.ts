import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthSessionService } from './auth-session.service';

describe('AuthSessionService', () => {
  let service: AuthSessionService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthSessionService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('restores the browser session once and keeps the safe profile in memory', () => {
    let firstUserRole = '';
    let secondUserRole = '';

    service.ensureSession().subscribe((user) => firstUserRole = user?.role ?? '');
    service.ensureSession().subscribe((user) => secondUserRole = user?.role ?? '');

    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/session');
    request.flush({
      id: 1,
      email: 'root@cacaomarket.local',
      login: 'root',
      prenom: 'Root',
      nom: 'System',
      role: 'ADMINISTRATEUR'
    });

    expect(firstUserRole).toBe('ADMINISTRATEUR');
    expect(secondUserRole).toBe('ADMINISTRATEUR');
    expect(service.user()?.login).toBe('root');
  });

  it('clears the profile when the server session cannot be restored', () => {
    service.ensureSession().subscribe((user) => expect(user).toBeNull());

    const request = httpTesting.expectOne('/cacaomarketcm/api/auth/session');
    request.flush({ code: 'SESSION_NOT_AUTHENTICATED' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.user()).toBeNull();
  });
});
