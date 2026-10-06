package cm.odigital.serviceconnectmarket.market.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Accept/refuse decision of the counterpart for a negotiation or a visit proposal. */
public record DecisionRequest(
    @NotBlank @Pattern(regexp = "(?i)ACCEPTER|REFUSER") String decision
) {

    /** True when the counterpart accepted, false when it refused. */
    public boolean accepts() {
        return "ACCEPTER".equalsIgnoreCase(decision.trim());
    }
}
