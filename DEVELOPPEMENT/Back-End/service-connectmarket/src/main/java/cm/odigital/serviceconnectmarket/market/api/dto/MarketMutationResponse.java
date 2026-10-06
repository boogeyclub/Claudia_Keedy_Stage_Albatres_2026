package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/** Result of a creation or of a status change. {@code id} is null when no record was created. */
public record MarketMutationResponse(
    Long id,
    String statut,
    String message
) {
}
