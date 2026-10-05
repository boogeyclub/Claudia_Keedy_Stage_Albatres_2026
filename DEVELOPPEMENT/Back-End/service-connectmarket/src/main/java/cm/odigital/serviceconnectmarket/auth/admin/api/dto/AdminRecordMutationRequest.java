package cm.odigital.serviceconnectmarket.auth.admin.api.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Values are validated against a strict per-table allow-list in AdminTableService. This DTO never
 * permits callers to submit arbitrary SQL columns or raw security values.
 */
public record AdminRecordMutationRequest(Map<String, Object> values) {

    public AdminRecordMutationRequest {
        // Preserve a malformed null field long enough for the service allow-list to return a safe
        // 400 validation error rather than leaking an implementation exception.
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
