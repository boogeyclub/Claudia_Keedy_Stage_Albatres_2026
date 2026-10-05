package cm.odigital.serviceconnectmarket.market.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Visit proposal (rendez-vous) used to see the cocoa on site. */
public record AppointmentRequest(
    @NotNull @Future Instant dateProposee,
    @Size(max = 200) String lieu,
    @Size(max = 500) String note
) {
}
