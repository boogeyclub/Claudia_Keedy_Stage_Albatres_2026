import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { AdminTableManagementComponent } from './admin-table-management';

describe('AdminTableManagementComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminTableManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ table: 'sessions_utilisateur' }) } }
        }
      ]
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('renders only the safe session audit projection and a revocation action', () => {
    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(AdminTableManagementComponent);
    fixture.detectChanges();

    const request = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/sessions_utilisateur');
    request.flush({
      table: 'sessions_utilisateur',
      records: [{
        id: 44,
        utilisateurLogin: 'amina-cocoa',
        utilisateurEmail: 'amina@example.com',
        browserLabel: 'Google Chrome on Windows',
        rememberMe: true,
        lastSeenAt: '2026-09-29T10:00:00Z',
        expiresAt: '2026-09-29T10:30:00Z',
        invalidatedAt: null,
        sessionHash: 'must-not-render'
      }]
    });
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    expect(nativeElement.textContent).toContain('Google Chrome on Windows');
    expect(nativeElement.textContent).toContain('Revoke session');
    expect(nativeElement.textContent).not.toContain('must-not-render');
    expect(nativeElement.querySelector('input')).toBeNull();
    expect(TestBed.inject(Title).getTitle()).toBe('CacaoMarketCM | Browser sessions');
  });

  it('distinguishes a reached-but-failing backend from a browser connection failure', () => {
    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(AdminTableManagementComponent);
    fixture.detectChanges();

    const request = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/sessions_utilisateur');
    request.flush(
      { code: 'DATA_ACCESS_UNAVAILABLE', message: 'The protected data service is temporarily unavailable.' },
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: new HttpHeaders({ 'X-Request-Id': 'admin-table-503' })
      }
    );
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('request ID admin-table-503');
  });

  it('shows a connection-specific message when the browser cannot reach the API', () => {
    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(AdminTableManagementComponent);
    fixture.detectChanges();

    const request = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/sessions_utilisateur');
    request.error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('could not reach the configured API');
  });

  it('asks the administrator to sign in again after an authentication response', () => {
    TestBed.inject(TranslationService).setLanguage('en');
    const fixture = TestBed.createComponent(AdminTableManagementComponent);
    fixture.detectChanges();

    const request = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/sessions_utilisateur');
    request.flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('session is no longer active');
  });
});
