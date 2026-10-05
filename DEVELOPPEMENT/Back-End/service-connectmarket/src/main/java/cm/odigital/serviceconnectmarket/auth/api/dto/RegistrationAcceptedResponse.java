package cm.odigital.serviceconnectmarket.auth.api.dto;

import java.time.Instant;

public record RegistrationAcceptedResponse(String email, Instant expiresAt) {
}
