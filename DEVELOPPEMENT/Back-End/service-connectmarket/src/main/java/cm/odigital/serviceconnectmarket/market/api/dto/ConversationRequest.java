package cm.odigital.serviceconnectmarket.market.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Opens (or reuses) the buyer's thread about one lot, with an optional first message. */
public record ConversationRequest(
    @NotNull Long lotId,
    @Size(max = 2000) String message
) {
}
