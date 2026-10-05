package cm.odigital.serviceconnectmarket.auth.messaging;

import java.time.Instant;

import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;

public record RegistrationConfirmationMessage(
    String recipientEmail,
    String recipientFirstName,
    String confirmationUrl,
    Instant expiresAt,
    RegistrationLanguage language
) {
}
