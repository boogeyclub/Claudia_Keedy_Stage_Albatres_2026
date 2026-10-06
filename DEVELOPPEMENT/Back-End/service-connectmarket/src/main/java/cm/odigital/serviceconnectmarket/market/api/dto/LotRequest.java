package cm.odigital.serviceconnectmarket.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creation/update payload of a catalogue lot. The seller states the total volume; the API stores
 * the available volume separately because accepted negotiations reduce it over time.
 */
public record LotRequest(
    @NotBlank @Size(max = 150) String titre,
    @Size(max = 2000) String description,
    @NotNull Long typeCacaoId,
    @NotNull @DecimalMin("0.01") BigDecimal quantiteKg,
    @NotNull @DecimalMin("0.01") BigDecimal prixKg,
    @Size(min = 3, max = 3) String devise,
    @NotNull Long regionId,
    Long villeId,
    @Size(max = 200) String localisation,
    @DecimalMin("-90.0") BigDecimal latitude,
    @DecimalMin("-180.0") BigDecimal longitude,
    LocalDate dateRecolte,
    LocalDate dateDisponibilite,
    @Size(max = 500) String photoUrl,
    Boolean publier
) {
}
