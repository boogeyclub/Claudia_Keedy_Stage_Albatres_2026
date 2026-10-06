package cm.odigital.serviceconnectmarket.market.api.dto;

import jakarta.validation.constraints.Size;

/** Buyer asking the seller to share the exact position of the article. */
public record PositionRequest(
    @Size(max = 500) String message
) {
}
