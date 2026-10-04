package cm.odigital.serviceconnectmarket.auth.messaging;

import java.time.Instant;

import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;

public record PasswordResetMessage(
    String recipientEmail,
    String recipientFirstName,
    String resetUrl,
    Instant expiresAt,
    RegistrationLanguage language
) {
}
