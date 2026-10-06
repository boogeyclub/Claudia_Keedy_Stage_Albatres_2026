package cm.odigital.serviceconnectmarket.market.messaging;

/**
 * Sends the "something started" emails of the market.
 *
 * <p>Important difference with the account emails: delivery is <strong>best effort</strong>. A
 * missing or refusing SMTP configuration must never cancel a message, a negotiation or a visit —
 * those are stored and pushed to the real-time stream, and the mail is only a nudge. Implementations
 * therefore log the failure and return instead of throwing.
 */
public interface MarketMessagingService {

    void send(MarketNotification notification);
}
