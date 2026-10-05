package cm.odigital.serviceconnectmarket.auth.admin.api.dto;

import java.util.List;
import java.util.Map;

/**
 * A safe, table-specific projection for the administrator UI. Sensitive database fields such as
 * password hashes, session hashes, and token hashes are intentionally omitted by the repository.
 */
public record AdminTableRowsResponse(String table, List<Map<String, Object>> records) {
}
