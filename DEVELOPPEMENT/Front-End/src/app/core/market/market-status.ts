/**
 * Translation keys and visual tones for every market status.
 *
 * The database stores stable codes (`PUBLIE`, `ACCEPTEE`, `PROPOSE`, …); the interface always shows
 * a translated label, so no code leaks into the pages.
 */

export type MarketStatusTone = 'neutral' | 'info' | 'pending' | 'success' | 'danger' | 'muted';

export interface MarketStatusDescriptor {
  readonly labelKey: string;
  readonly tone: MarketStatusTone;
}

const LOT_STATUSES: Readonly<Record<string, MarketStatusDescriptor>> = {
  BROUILLON: { labelKey: 'market.status.lot.draft', tone: 'muted' },
  PUBLIE: { labelKey: 'market.status.lot.published', tone: 'success' },
  RESERVE: { labelKey: 'market.status.lot.reserved', tone: 'pending' },
  VENDU: { labelKey: 'market.status.lot.sold', tone: 'info' },
  ARCHIVE: { labelKey: 'market.status.lot.archived', tone: 'neutral' }
};

const CONVERSATION_STATUSES: Readonly<Record<string, MarketStatusDescriptor>> = {
  OUVERTE: { labelKey: 'market.status.conversation.open', tone: 'info' },
  EN_NEGOCIATION: { labelKey: 'market.status.conversation.negotiating', tone: 'pending' },
  ACCORD: { labelKey: 'market.status.conversation.agreed', tone: 'success' },
  CLOTUREE: { labelKey: 'market.status.conversation.closed', tone: 'muted' }
};

const NEGOTIATION_STATUSES: Readonly<Record<string, MarketStatusDescriptor>> = {
  PROPOSEE: { labelKey: 'market.status.negotiation.proposed', tone: 'pending' },
  ACCEPTEE: { labelKey: 'market.status.negotiation.accepted', tone: 'success' },
  REFUSEE: { labelKey: 'market.status.negotiation.refused', tone: 'danger' },
  ANNULEE: { labelKey: 'market.status.negotiation.cancelled', tone: 'muted' },
  EXPIREE: { labelKey: 'market.status.negotiation.expired', tone: 'neutral' }
};

const APPOINTMENT_STATUSES: Readonly<Record<string, MarketStatusDescriptor>> = {
  PROPOSE: { labelKey: 'market.status.appointment.proposed', tone: 'pending' },
  ACCEPTE: { labelKey: 'market.status.appointment.accepted', tone: 'success' },
  REFUSE: { labelKey: 'market.status.appointment.refused', tone: 'danger' },
  ANNULE: { labelKey: 'market.status.appointment.cancelled', tone: 'muted' }
};

const UNKNOWN_STATUS: MarketStatusDescriptor = { labelKey: 'market.status.unknown', tone: 'neutral' };

export function lotStatus(status: string | null | undefined): MarketStatusDescriptor {
  return describe(status, LOT_STATUSES);
}

export function conversationStatus(status: string | null | undefined): MarketStatusDescriptor {
  return describe(status, CONVERSATION_STATUSES);
}

export function negotiationStatus(status: string | null | undefined): MarketStatusDescriptor {
  return describe(status, NEGOTIATION_STATUSES);
}

export function appointmentStatus(status: string | null | undefined): MarketStatusDescriptor {
  return describe(status, APPOINTMENT_STATUSES);
}

export function isOpenNegotiation(status: string | null | undefined): boolean {
  return status === 'PROPOSEE';
}

export function isOpenAppointment(status: string | null | undefined): boolean {
  return status === 'PROPOSE';
}

function describe(
  status: string | null | undefined,
  catalog: Readonly<Record<string, MarketStatusDescriptor>>
): MarketStatusDescriptor {
  return (status && catalog[status]) || UNKNOWN_STATUS;
}
