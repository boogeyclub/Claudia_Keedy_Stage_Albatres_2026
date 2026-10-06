package cm.odigital.serviceconnectmarket.market.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Price/volume proposal sent inside a conversation thread. */
public record NegotiationRequest(
    @NotNull @DecimalMin("0.01") BigDecimal prixKg,
    @NotNull @DecimalMin("0.01") BigDecimal quantiteKg,
    @Size(max = 500) String message
) {
}
