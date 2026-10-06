package cm.odigital.serviceconnectmarket.market.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Visit proposal (rendez-vous) used to see the cocoa on site.
 *
 * <p>The proposer pins the meeting point on a map. The pin stays an ordinary proposal: it is only
 * acted upon (status CONFIRME) once both participants validated it, each from their own workspace.
 */
public record AppointmentRequest(
    @NotNull @Future Instant dateProposee,
    @Size(max = 200) String lieu,
    @Size(max = 200) String lieuLibelle,
    @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
    @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
    @Size(max = 500) String note
) {

    /** True when the proposer pinned the meeting point on the map. */
    public boolean hasPoint() {
        return latitude != null && longitude != null;
    }
}
