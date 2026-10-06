package cm.odigital.serviceconnectmarket.market.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A participant's answer about the GPS pin of a visit.
 *
 * <p>{@code VALIDER} records the approval of the pinned point; when both participants have approved,
 * the visit becomes {@code CONFIRME}. {@code REFUSER} clears both approvals, so the pin has to be
 * proposed again before the visit can be set.
 */
public record PointDecisionRequest(
    @NotBlank @Pattern(regexp = "(?i)VALIDER|REFUSER") String decision
) {

    public boolean approves() {
        return "VALIDER".equalsIgnoreCase(decision.trim());
    }
}
