/**
 * Market models mirroring the `/api/market` payloads.
 *
 * The backend serialises JDBC `NUMERIC` columns as JSON numbers and `DATE`/`TIMESTAMPTZ` columns as
 * ISO strings, so amounts are typed `number` and dates `string`.
 */

export type LotStatus = 'BROUILLON' | 'PUBLIE' | 'RESERVE' | 'VENDU' | 'ARCHIVE';
export type ConversationStatus = 'OUVERTE' | 'EN_NEGOCIATION' | 'ACCORD' | 'CLOTUREE';
export type NegotiationStatus = 'PROPOSEE' | 'ACCEPTEE' | 'REFUSEE' | 'ANNULEE' | 'EXPIREE';
export type AppointmentStatus = 'PROPOSE' | 'ACCEPTE' | 'REFUSE' | 'ANNULE' | 'CONFIRME';

/** Life cycle of the seller's answer to a position request. */
export type PositionShareStatus = 'DEMANDE' | 'ACCEPTEE' | 'REFUSEE' | 'REVOQUEE';

export interface MarketVille {
  readonly id: number;
  readonly regionId: number;
  readonly nom: string;
}

export interface MarketRegion {
  readonly id: number;
  readonly code: string;
  readonly nom: string;
  readonly villes: readonly MarketVille[];
}

export interface MarketCacaoType {
  readonly id: number;
  readonly code: string;
  readonly nom: string;
  readonly description: string | null;
}

export interface MarketReference {
  readonly regions: readonly MarketRegion[];
  readonly cacaoTypes: readonly MarketCacaoType[];
}

export interface MarketLot {
  readonly id: number;
  readonly vendeurId: number;
  readonly typeCacaoId: number;
  readonly titre: string;
  readonly description: string | null;
  readonly quantiteKg: number;
  readonly quantiteDisponibleKg: number;
  readonly prixKg: number;
  readonly devise: string;
  readonly regionId: number;
  readonly villeId: number | null;
  readonly localisation: string | null;
  readonly latitude: number | null;
  readonly longitude: number | null;
  readonly dateRecolte: string | null;
  readonly dateDisponibilite: string | null;
  readonly statut: LotStatus;
  readonly dateCreation: string;
  readonly datePublication: string | null;
  readonly dateMiseAJour: string;
  readonly typeCode: string;
  readonly typeNom: string;
  readonly regionCode: string;
  readonly regionNom: string;
  readonly villeNom: string | null;
  readonly vendeurPrenom: string;
  readonly vendeurNom: string;
  readonly vendeurLogin: string;
  readonly photoUrl: string | null;
}

export interface MarketLotMedia {
  readonly id: number;
  readonly url: string;
  readonly legende: string | null;
  readonly position: number;
}

export interface MarketConversation {
  readonly id: number;
  readonly lotId: number;
  readonly clientId: number;
  readonly vendeurId: number;
  readonly statut: ConversationStatus;
  readonly dateCreation: string;
  readonly dernierMessageAt: string | null;
  readonly lotTitre: string;
  readonly lotPrixKg: number;
  readonly lotDevise: string;
  readonly lotStatut: LotStatus;
  readonly lotQuantiteDisponibleKg: number;
  readonly clientPrenom: string;
  readonly clientNom: string;
  readonly clientLogin: string;
  readonly vendeurPrenom: string;
  readonly vendeurNom: string;
  readonly vendeurLogin: string;
  readonly dernierMessage: string | null;
  readonly nonLus: number;
}

export interface MarketMessage {
  readonly id: number;
  readonly expediteurId: number;
  readonly contenu: string;
  readonly type: 'TEXTE' | 'SYSTEME';
  readonly dateEnvoi: string;
  readonly luAt: string | null;
}

export interface MarketNegotiation {
  readonly id: number;
  readonly conversationId?: number;
  readonly proposeurId: number;
  readonly prixKg: number;
  readonly quantiteKg: number;
  readonly message: string | null;
  readonly statut: NegotiationStatus;
  readonly dateCreation: string;
  readonly dateReponse: string | null;
  readonly expiresAt: string | null;
}

export interface MarketAppointment {
  readonly id: number;
  readonly conversationId?: number;
  readonly proposeurId: number;
  readonly dateProposee: string;
  readonly lieu: string | null;
  /** Name of the pinned point, as the proposer typed it. */
  readonly lieuLibelle: string | null;
  readonly latitude: number | null;
  readonly longitude: number | null;
  /** Set when the author of the proposal approved the pin (pinning it counts as approving it). */
  readonly pointValideProposeurAt: string | null;
  /** Set when the invited participant approved the pin. Both are required to confirm the visit. */
  readonly pointValideInviteAt: string | null;
  readonly note: string | null;
  readonly statut: AppointmentStatus;
  readonly dateCreation: string;
  readonly dateReponse: string | null;
}

/**
 * A position request inside one thread: the buyer asks, the seller answers with a GPS point or a
 * refusal. The active row is part of the thread detail, so both participants see the same pin.
 */
export interface MarketPositionShare {
  readonly id: number;
  readonly conversationId: number;
  readonly demandeurId: number;
  readonly destinataireId: number;
  readonly statut: PositionShareStatus;
  readonly latitude: number | null;
  readonly longitude: number | null;
  readonly libelle: string | null;
  readonly message: string | null;
  readonly dateDemande: string;
  readonly dateReponse: string | null;
}

/** One real-time signal pushed by the API over Server-Sent Events. */
export interface MarketRealtimeEvent {
  readonly type: 'MESSAGE' | 'NEGOCIATION' | 'RENDEZ_VOUS' | 'POSITION';
  readonly conversationId: number;
  /** Account that acted, so a tab can ignore the echo of its own actions. */
  readonly authorId: number;
  readonly lotId: number;
  readonly lotTitre: string;
  readonly authorName: string;
  readonly preview: string;
  readonly at: string;
}

/** A GPS point submitted to the API. */
export interface GeoPointPayload {
  latitude: number;
  longitude: number;
  libelle?: string | null;
}

export interface CatalogueFilters {
  recherche?: string | null;
  regionId?: number | null;
  villeId?: number | null;
  typeCacaoId?: number | null;
  recolteFrom?: string | null;
  recolteTo?: string | null;
  disponibiliteFrom?: string | null;
  prixMax?: number | null;
  quantiteMin?: number | null;
}

export interface LotPayload {
  titre: string;
  description?: string | null;
  typeCacaoId: number;
  quantiteKg: number;
  prixKg: number;
  devise: string;
  regionId: number;
  villeId?: number | null;
  localisation?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  dateRecolte?: string | null;
  dateDisponibilite?: string | null;
  photoUrl?: string | null;
  publier?: boolean;
}

export interface CatalogueResponse {
  readonly scope: string;
  readonly lots: readonly MarketLot[];
}

export interface LotDetailResponse {
  readonly lot: MarketLot;
  readonly medias: readonly MarketLotMedia[];
}

export interface ConversationListResponse {
  readonly conversations: readonly MarketConversation[];
}

export interface ConversationDetailResponse {
  readonly conversation: MarketConversation;
  readonly messages: readonly MarketMessage[];
  readonly negociations: readonly MarketNegotiation[];
  readonly rendezVous: readonly MarketAppointment[];
  /** Active position request of the thread, or null when there is none. */
  readonly position: MarketPositionShare | null;
}

export interface DealNegotiation extends MarketNegotiation {
  readonly lotId: number;
  readonly lotTitre: string;
  readonly lotDevise: string;
  readonly lotStatut: LotStatus;
  readonly contrepartie: string;
}

export interface DealAppointment extends MarketAppointment {
  readonly lotId: number;
  readonly lotTitre: string;
  readonly lotDevise: string;
  readonly contrepartie: string;
}

export interface DealsResponse {
  readonly negociations: readonly DealNegotiation[];
  readonly rendezVous: readonly DealAppointment[];
}

export interface MarketMutationResponse {
  readonly id: number | null;
  readonly statut: string | null;
  readonly message: string;
}
