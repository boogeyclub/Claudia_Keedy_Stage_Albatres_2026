package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/** Negotiations and visit requests of the signed-in account, across every conversation. */
public record MarketDealsResponse(
    List<Map<String, Object>> negociations,
    List<Map<String, Object>> rendezVous
) {
}
