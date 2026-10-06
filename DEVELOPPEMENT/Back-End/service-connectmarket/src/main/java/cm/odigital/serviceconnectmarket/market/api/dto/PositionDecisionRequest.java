package cm.odigital.serviceconnectmarket.market.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The seller's answer to a position request: share the exact spot, or refuse.
 *
 * <p>Accepting requires the pin, refusing forbids it — the API checks that pairing so a refusal can
 * never leak a coordinate.
 */
public record PositionDecisionRequest(
    @NotBlank @Pattern(regexp = "(?i)ACCEPTER|REFUSER") String decision,
    @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
    @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
    @Size(max = 200) String libelle
) {

    public boolean accepts() {
        return "ACCEPTER".equalsIgnoreCase(decision.trim());
    }
}
