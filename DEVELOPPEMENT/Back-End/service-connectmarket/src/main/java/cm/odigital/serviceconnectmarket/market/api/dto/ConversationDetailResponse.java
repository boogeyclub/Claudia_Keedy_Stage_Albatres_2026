package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/** Thread content: messages, proposals and visit requests of one lot conversation. */
public record ConversationDetailResponse(
    Map<String, Object> conversation,
    List<Map<String, Object>> messages,
    List<Map<String, Object>> negociations,
    List<Map<String, Object>> rendezVous
) {
}
