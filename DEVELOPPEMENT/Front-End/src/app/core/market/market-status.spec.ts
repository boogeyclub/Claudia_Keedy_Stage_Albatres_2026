import {
  appointmentStatus,
  conversationStatus,
  isOpenAppointment,
  isOpenNegotiation,
  lotStatus,
  needsPointValidation,
  negotiationStatus,
  positionStatus
} from './market-status';

describe('market status helpers', () => {
  it('translates every status the API can send', () => {
    expect(lotStatus('PUBLIE')).toEqual({ labelKey: 'market.status.lot.published', tone: 'success' });
    expect(conversationStatus('ACCORD')).toEqual({ labelKey: 'market.status.conversation.agreed', tone: 'success' });
    expect(negotiationStatus('EXPIREE')).toEqual({ labelKey: 'market.status.negotiation.expired', tone: 'neutral' });
    expect(positionStatus('ACCEPTEE')).toEqual({ labelKey: 'market.status.position.shared', tone: 'success' });
  });

  it('marks a visit as confirmed only once both sides approved the point', () => {
    expect(appointmentStatus('CONFIRME')).toEqual({ labelKey: 'market.status.appointment.confirmed', tone: 'success' });
    expect(appointmentStatus('ACCEPTE')).toEqual({ labelKey: 'market.status.appointment.accepted', tone: 'info' });
  });

  it('never shows a raw code for an unknown or missing status', () => {
    expect(lotStatus('INCONNU')).toEqual({ labelKey: 'market.status.unknown', tone: 'neutral' });
    expect(lotStatus(null)).toEqual({ labelKey: 'market.status.unknown', tone: 'neutral' });
    expect(lotStatus(undefined)).toEqual({ labelKey: 'market.status.unknown', tone: 'neutral' });
  });

  it('keeps a visit blocking while it waits for an answer or a point approval', () => {
    expect(isOpenAppointment('PROPOSE')).toBe(true);
    expect(isOpenAppointment('ACCEPTE')).toBe(true);
    expect(isOpenAppointment('CONFIRME')).toBe(false);
    expect(isOpenAppointment('REFUSE')).toBe(false);
    expect(isOpenNegotiation('PROPOSEE')).toBe(true);
    expect(isOpenNegotiation('ACCEPTEE')).toBe(false);
  });

  it('asks for a point approval only when a pin exists and one side is missing', () => {
    const point = {
      statut: 'ACCEPTE',
      latitude: 4.0511,
      pointValideProposeurAt: '2026-10-01T10:00:00Z',
      pointValideInviteAt: null
    };
    expect(needsPointValidation(point)).toBe(true);
    expect(needsPointValidation({ ...point, pointValideInviteAt: '2026-10-01T11:00:00Z' })).toBe(false);
    expect(needsPointValidation({ ...point, latitude: null })).toBe(false);
    expect(needsPointValidation({ ...point, statut: 'PROPOSE' })).toBe(false);
  });
});
