package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/** Filter values: regions with their cities, and the cocoa types accepted by the catalogue. */
public record MarketReferenceResponse(
    List<Map<String, Object>> regions,
    List<Map<String, Object>> cacaoTypes
) {
}
