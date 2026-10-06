import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RuntimeConfigurationService } from '../config/runtime-configuration.service';
import {
  CatalogueFilters,
  CatalogueResponse,
  ConversationDetailResponse,
  ConversationListResponse,
  DealsResponse,
  GeoPointPayload,
  LotDetailResponse,
  LotPayload,
  MarketMutationResponse,
  MarketReference
} from './market-models';

/**
 * Typed access to the catalogue, messaging, negotiation and visit endpoints.
 *
 * Every call is credentialed: the backend authorises each request from the server session and the
 * account type, never from anything the browser sends.
 */
@Injectable({ providedIn: 'root' })
export class MarketApiService {
  private readonly http = inject(HttpClient);
  private readonly runtimeConfiguration = inject(RuntimeConfigurationService);

  private get apiRoot(): string {
    return `${this.runtimeConfiguration.apiBaseUrl}/market`;
  }

  reference(): Observable<MarketReference> {
    return this.http.get<MarketReference>(`${this.apiRoot}/reference`, { withCredentials: true });
  }

  catalogue(filters: CatalogueFilters): Observable<CatalogueResponse> {
    return this.http.get<CatalogueResponse>(`${this.apiRoot}/lots`, {
      params: this.filterParams(filters),
      withCredentials: true
    });
  }

  lot(lotId: number): Observable<LotDetailResponse> {
    return this.http.get<LotDetailResponse>(`${this.apiRoot}/lots/${lotId}`, { withCredentials: true });
  }

  myLots(): Observable<CatalogueResponse> {
    return this.http.get<CatalogueResponse>(`${this.apiRoot}/vendeur/lots`, { withCredentials: true });
  }

  createLot(payload: LotPayload): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(`${this.apiRoot}/vendeur/lots`, payload, { withCredentials: true });
  }

  updateLot(lotId: number, payload: LotPayload): Observable<MarketMutationResponse> {
    return this.http.put<MarketMutationResponse>(`${this.apiRoot}/vendeur/lots/${lotId}`, payload, { withCredentials: true });
  }

  changeLotStatus(lotId: number, statut: string): Observable<MarketMutationResponse> {
    return this.http.put<MarketMutationResponse>(
      `${this.apiRoot}/vendeur/lots/${lotId}/statut`,
      { statut },
      { withCredentials: true }
    );
  }

  openConversation(lotId: number, message: string | null): Observable<ConversationDetailResponse> {
    return this.http.post<ConversationDetailResponse>(
      `${this.apiRoot}/conversations`,
      { lotId, message },
      { withCredentials: true }
    );
  }

  conversations(): Observable<ConversationListResponse> {
    return this.http.get<ConversationListResponse>(`${this.apiRoot}/conversations`, { withCredentials: true });
  }

  deals(): Observable<DealsResponse> {
    return this.http.get<DealsResponse>(`${this.apiRoot}/deals`, { withCredentials: true });
  }

  conversation(conversationId: number): Observable<ConversationDetailResponse> {
    return this.http.get<ConversationDetailResponse>(`${this.apiRoot}/conversations/${conversationId}`, {
      withCredentials: true
    });
  }

  sendMessage(conversationId: number, contenu: string): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(
      `${this.apiRoot}/conversations/${conversationId}/messages`,
      { contenu },
      { withCredentials: true }
    );
  }

  proposeNegotiation(
    conversationId: number,
    payload: { prixKg: number; quantiteKg: number; message: string | null }
  ): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(
      `${this.apiRoot}/conversations/${conversationId}/negociations`,
      payload,
      { withCredentials: true }
    );
  }

  decideNegotiation(negociationId: number, accepted: boolean): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(
      `${this.apiRoot}/negociations/${negociationId}/decision`,
      { decision: accepted ? 'ACCEPTER' : 'REFUSER' },
      { withCredentials: true }
    );
  }

  cancelNegotiation(negociationId: number): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(
      `${this.apiRoot}/negociations/${negociationId}/annulation`,
      {},
      { withCredentials: true }
    );
  }

  proposeAppointment(
    conversationId: number,
    payload: {
      dateProposee: string;
      lieu: string | null;
      lieuLibelle: string | null;
      latitude: number;
      longitude: number;
      note: string | null;
    }
  ): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(
      `${this.apiRoot}/conversations/${conversationId}/rendez-vous`,
      payload,
      { withCredentials: true }
    );
  }

  decideAppointment(rendezVousId: number, accepted: boolean): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(
      `${this.apiRoot}/rendez-vous/${rendezVousId}/decision`,
      { decision: accepted ? 'ACCEPTER' : 'REFUSER' },
      { withCredentials: true }
    );
  }

  cancelAppointment(rendezVousId: number): Observable<MarketMutationResponse> {
    return this.http.post<MarketMutationResponse>(
      `${this.apiRoot}/rendez-vous/${rendezVousId}/annulation`,
      {},
      { withCredentials: true }
    );
  }

  /** The buyer asks the seller to share the exact position of the lot. */
  requestPositionShare(conversationId: number, message: string | null): Observable<ConversationDetailResponse> {
    return this.http.post<ConversationDetailResponse>(
      `${this.apiRoot}/conversations/${conversationId}/position-demande`,
      { message },
      { withCredentials: true }
    );
  }

  /** The seller shares the exact point, or refuses the request. */
  decidePositionShare(
    partageId: number,
    accepted: boolean,
    point: GeoPointPayload | null
  ): Observable<ConversationDetailResponse> {
    return this.http.post<ConversationDetailResponse>(
      `${this.apiRoot}/positions/${partageId}/decision`,
      {
        decision: accepted ? 'ACCEPTER' : 'REFUSER',
        latitude: accepted ? point?.latitude ?? null : null,
        longitude: accepted ? point?.longitude ?? null : null,
        libelle: accepted ? point?.libelle ?? null : null
      },
      { withCredentials: true }
    );
  }

  revokePositionShare(partageId: number): Observable<ConversationDetailResponse> {
    return this.http.post<ConversationDetailResponse>(
      `${this.apiRoot}/positions/${partageId}/revocation`,
      {},
      { withCredentials: true }
    );
  }

  /** Moves the pin of a visit; the invited participant has to approve it again. */
  moveAppointmentPoint(rendezVousId: number, point: GeoPointPayload): Observable<ConversationDetailResponse> {
    return this.http.put<ConversationDetailResponse>(
      `${this.apiRoot}/rendez-vous/${rendezVousId}/point`,
      point,
      { withCredentials: true }
    );
  }

  /** Approves or refuses the pin of a visit; two approvals confirm the visit. */
  decideAppointmentPoint(rendezVousId: number, approved: boolean): Observable<ConversationDetailResponse> {
    return this.http.post<ConversationDetailResponse>(
      `${this.apiRoot}/rendez-vous/${rendezVousId}/point-validation`,
      { decision: approved ? 'VALIDER' : 'REFUSER' },
      { withCredentials: true }
    );
  }

  /** Absolute URL of the live stream, for the browser's EventSource. */
  get eventsUrl(): string {
    return `${this.apiRoot}/events`;
  }

  private filterParams(filters: CatalogueFilters): HttpParams {
    let params = new HttpParams();
    const entries: readonly (readonly [string, unknown])[] = [
      ['recherche', filters.recherche],
      ['regionId', filters.regionId],
      ['villeId', filters.villeId],
      ['typeCacaoId', filters.typeCacaoId],
      ['recolteFrom', filters.recolteFrom],
      ['recolteTo', filters.recolteTo],
      ['disponibiliteFrom', filters.disponibiliteFrom],
      ['prixMax', filters.prixMax],
      ['quantiteMin', filters.quantiteMin]
    ];

    for (const [name, value] of entries) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(name, String(value));
      }
    }
    return params;
  }
}
