package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/** Inbox of the signed-in buyer or seller; {@code nonLus} counts the messages waiting for them. */
public record ConversationListResponse(
    List<Map<String, Object>> conversations
) {
}
