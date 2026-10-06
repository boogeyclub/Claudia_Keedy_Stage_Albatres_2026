package cm.odigital.serviceconnectmarket.market.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A GPS pin submitted by a user.
 *
 * <p>Latitude and longitude always travel together and inside valid bounds; the bounds are also a
 * database CHECK constraint, so a malformed client cannot store an unusable point.
 */
public record GeoPointRequest(
    @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
    @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
    @Size(max = 200) String libelle
) {
}
