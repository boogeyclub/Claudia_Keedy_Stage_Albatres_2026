package cm.odigital.serviceconnectmarket.market.messaging;

import java.time.Instant;

import cm.odigital.serviceconnectmarket.market.domain.MarketEventType;

/**
 * One market email, ready to be rendered by the mail service.
 *
 * <p>It only contains what the recipient is allowed to know: the title of the article, the name of
 * the counterpart, and the detail of the event (price, date, place…). No identifier, no token and no
 * third-party address ever reaches the template.
 */
public record MarketNotification(
    MarketEventType type,
    String recipientEmail,
    String recipientFirstName,
    String counterpartName,
    String lotTitle,
    String detail,
    String conversationUrl,
    Instant occurredAt
) {
}
