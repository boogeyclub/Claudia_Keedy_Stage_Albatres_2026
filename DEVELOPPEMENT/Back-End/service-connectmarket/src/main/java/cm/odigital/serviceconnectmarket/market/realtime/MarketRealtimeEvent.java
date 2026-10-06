package cm.odigital.serviceconnectmarket.market.realtime;

import java.time.Instant;

/**
 * One real-time signal pushed to a browser.
 *
 * <p>It carries just enough to update a screen without another round trip: what changed, in which
 * thread, and a short human-readable preview for the notification toast. It never carries a
 * password, a token or another participant's private data — the browser still reads the thread
 * through the regular API when it needs the full content.
 *
 * @param type one of {@code MESSAGE}, {@code NEGOCIATION}, {@code RENDEZ_VOUS} or {@code POSITION}
 * @param conversationId the thread the change belongs to
 * @param authorId the account that acted, so a tab can ignore its own actions
 * @param lotId the article of that thread
 * @param lotTitre the article title, so a toast can name it without a lookup
 * @param authorName the participant who acted
 * @param preview a short summary of the change
 * @param at when the change happened, server side
 */
public record MarketRealtimeEvent(
    String type,
    long conversationId,
    long authorId,
    long lotId,
    String lotTitre,
    String authorName,
    String preview,
    Instant at
) {
}
