package cm.odigital.serviceconnectmarket.market.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Sale life-cycle action of a lot. RESERVE is never requested by the seller: it is set by the API
 * when a negotiation is accepted.
 */
public record LotStatusRequest(
    @NotBlank @Pattern(regexp = "PUBLIE|ARCHIVE|VENDU") String statut
) {
}
