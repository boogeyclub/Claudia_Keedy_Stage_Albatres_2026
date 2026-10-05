import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminApiService } from './admin-api.service';

describe('AdminApiService', () => {
  let service: AdminApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdminApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('uses credentialed, table-specific administration endpoints', () => {
    service.tableRows('sessions_utilisateur').subscribe((response) => {
      expect(response.records[0]?.['browserLabel']).toBe('Google Chrome on Windows');
      expect(response.records[0]?.['sessionHash']).toBeUndefined();
    });
    const rowsRequest = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/sessions_utilisateur');
    expect(rowsRequest.request.method).toBe('GET');
    expect(rowsRequest.request.withCredentials).toBe(true);
    rowsRequest.flush({
      table: 'sessions_utilisateur',
      records: [{ id: 4, browserLabel: 'Google Chrome on Windows', invalidatedAt: null }]
    });

    service.createRecord('basic_rights', { code: 'LOT-REVIEW', name: 'Review cocoa lots' }).subscribe();
    const createRequest = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/basic_rights');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.withCredentials).toBe(true);
    expect(createRequest.request.body).toEqual({ values: { code: 'LOT-REVIEW', name: 'Review cocoa lots' } });
    createRequest.flush({ status: 'created', message: 'Administrative table operation completed.' });

    service.updateRecord('utilisateurs', '17', { statut: 'SUSPENDU' }).subscribe();
    const updateRequest = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/utilisateurs/17');
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.withCredentials).toBe(true);
    expect(updateRequest.request.body).toEqual({ values: { statut: 'SUSPENDU' } });
    updateRequest.flush({ status: 'updated', message: 'Administrative table operation completed.' });

    service.removeRecord('type_utilisateur_basic_right', '3:8').subscribe();
    const removeRequest = httpTesting.expectOne('/cacaomarketcm/api/admin/tables/type_utilisateur_basic_right/3%3A8');
    expect(removeRequest.request.method).toBe('DELETE');
    expect(removeRequest.request.withCredentials).toBe(true);
    removeRequest.flush(null);
  });
});
