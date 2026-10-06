package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Thread content: messages, proposals, visit requests and the active position sharing of one lot
 * conversation.
 *
 * <p>The exact position is part of the thread on purpose: both participants are allowed to see it as
 * soon as the seller shares it, and nobody else can read a thread.
 */
public record ConversationDetailResponse(
    Map<String, Object> conversation,
    List<Map<String, Object>> messages,
    List<Map<String, Object>> negociations,
    List<Map<String, Object>> rendezVous,
    Map<String, Object> position
) {
}
