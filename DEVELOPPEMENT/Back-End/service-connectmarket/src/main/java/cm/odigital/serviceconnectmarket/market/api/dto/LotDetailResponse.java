package cm.odigital.serviceconnectmarket.market.api.dto;

import java.util.List;
import java.util.Map;

/** One lot with its pictures and the number of buyer conversations it generated. */
public record LotDetailResponse(
    Map<String, Object> lot,
    List<Map<String, Object>> medias
) {
}
