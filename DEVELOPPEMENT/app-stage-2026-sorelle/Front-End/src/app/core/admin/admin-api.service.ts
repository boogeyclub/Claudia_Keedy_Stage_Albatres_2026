import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RuntimeConfigurationService } from '../config/runtime-configuration.service';

/** Values are intentionally treated as unknown until a whitelisted UI column formats them. */
export type AdminRecord = Record<string, unknown>;

export interface AdminTableRowsResponse {
  table: string;
  records: readonly AdminRecord[];
}

export interface AdminMutationResponse {
  status: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly runtimeConfiguration = inject(RuntimeConfigurationService);

  private get apiRoot(): string {
    return this.runtimeConfiguration.apiBaseUrl;
  }

  tableRows(table: string): Observable<AdminTableRowsResponse> {
    return this.http.get<AdminTableRowsResponse>(this.tableUrl(table), { withCredentials: true });
  }

  createRecord(table: string, values: Record<string, unknown>): Observable<AdminMutationResponse> {
    return this.http.post<AdminMutationResponse>(this.tableUrl(table), { values }, { withCredentials: true });
  }

  updateRecord(table: string, recordId: string, values: Record<string, unknown>): Observable<AdminMutationResponse> {
    return this.http.put<AdminMutationResponse>(`${this.tableUrl(table)}/${encodeURIComponent(recordId)}`, { values }, { withCredentials: true });
  }

  removeRecord(table: string, recordId: string): Observable<void> {
    return this.http.delete<void>(`${this.tableUrl(table)}/${encodeURIComponent(recordId)}`, { withCredentials: true });
  }

  private tableUrl(table: string): string {
    return `${this.apiRoot}/admin/tables/${encodeURIComponent(table)}`;
  }
}
