import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { AdminTableManagementComponent } from './admin-table-management';

describe('AdminTableManagementComponent', () => {
  let httpTesting: HttpTestingController;

  async function createFixture(tableKey: string): Promise<ComponentFixture<AdminTableManagementComponent>> {
    await TestBed.configureTestingModule({
      imports: [AdminTableManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ table: tableKey }) } }
        }
      ]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    TestBed.inject(TranslationService).setLanguage('en');
    return TestBed.createComponent(AdminTableManagementComponent);
  }

  afterEach(() => httpTesting.verify());

  it('renders only the safe session audit projection and a revocation action', async () => {
    const fixture = await createFixture('sessions_utilisateur');
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

  it('distinguishes a reached-but-failing backend from a browser connection failure', async () => {
    const fixture = await createFixture('sessions_utilisateur');
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

  it('shows a connection-specific message when the browser cannot reach the API', async () => {
    const fixture = await createFixture('sessions_utilisateur');
    fixture.detectChanges();

    const request = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/sessions_utilisateur');
    request.error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('could not reach the configured API');
  });

  it('asks the administrator to sign in again after an authentication response', async () => {
    const fixture = await createFixture('sessions_utilisateur');
    fixture.detectChanges();

    const request = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/sessions_utilisateur');
    request.flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('session is no longer active');
  });

  it('never offers the buyer type when an administrator creates an account manually', async () => {
    const fixture = await createFixture('utilisateurs');
    fixture.detectChanges();

    httpTesting.expectOne('/cacaomarketcm/api/admin/tables/utilisateurs').flush({
      table: 'utilisateurs',
      records: []
    });
    httpTesting.expectOne('/cacaomarketcm/api/admin/tables/type_utilisateur').flush({
      table: 'type_utilisateur',
      records: [
        { id: 1, code: 'ADMINISTRATEUR', name: 'Administrateur' },
        { id: 2, code: 'VENDEUR', name: 'Vendeur' },
        { id: 3, code: 'CLIENT', name: 'Client' }
      ]
    });
    fixture.detectChanges();

    const addButton = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Add record'));
    addButton?.click();
    fixture.detectChanges();

    const options = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('option'))
      .map((option) => option.textContent?.trim());
    expect(options).toContain('ADMINISTRATEUR — Administrateur');
    expect(options).toContain('VENDEUR — Vendeur');
    expect(options).not.toContain('CLIENT — Client');
  });

  it('explains a business rejection instead of reporting an unexplained failure', async () => {
    const fixture = await createFixture('utilisateurs');
    fixture.detectChanges();

    httpTesting.expectOne('/cacaomarketcm/api/admin/tables/utilisateurs').flush({
      table: 'utilisateurs',
      records: []
    });
    httpTesting.expectOne('/cacaomarketcm/api/admin/tables/type_utilisateur').flush({
      table: 'type_utilisateur',
      records: [{ id: 2, code: 'VENDEUR', name: 'Vendeur' }]
    });
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    Array.from(host.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Add record'))?.click();
    fixture.detectChanges();

    setEditorValue(host, 'typeUtilisateurId', '2');
    setEditorValue(host, 'prenom', 'Amina');
    setEditorValue(host, 'nom', 'Ngassa');
    setEditorValue(host, 'email', 'amina@example.com');
    setEditorValue(host, 'login', 'amina-cocoa');
    setEditorValue(host, 'statut', 'ACTIF');
    setEditorValue(host, 'password', 'secure-passphrase');
    host.querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const save = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/utilisateurs');
    save.flush(
      {
        code: 'ADMIN_CLIENT_CREATION_REQUIRES_REGISTRATION',
        message: 'Create buyer accounts through the registration workflow so their required legal profile is recorded.'
      },
      { status: 409, statusText: 'Conflict' }
    );
    fixture.detectChanges();

    const notifications = TestBed.inject(NotificationService);
    expect(notifications.notifications().map((notification) => notification.key))
      .toContain('notifications.admin.errors.clientCreationRequiresRegistration');
    expect(notifications.notifications().map((notification) => notification.key))
      .not.toContain('notifications.admin.saveFailed');
    // The server text is never displayed: only the translated message is rendered.
    expect(host.textContent).not.toContain('their required legal profile is recorded');
  });
});

function setEditorValue(host: HTMLElement, fieldKey: string, value: string): void {
  const control = host.querySelector<HTMLInputElement | HTMLSelectElement>(`#admin-field-${fieldKey}`);
  if (!control) {
    throw new Error(`Editor control ${fieldKey} is missing.`);
  }
  control.value = value;
  control.dispatchEvent(new Event('input'));
  control.dispatchEvent(new Event('change'));
}
