package cm.odigital.serviceconnectmarket.auth.api.dto;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp) {
}
