package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/** Catalogue page: either the buyer board (published lots) or the seller's own lots. */
public record CatalogueResponse(
    String scope,
    List<Map<String, Object>> lots
) {
}
