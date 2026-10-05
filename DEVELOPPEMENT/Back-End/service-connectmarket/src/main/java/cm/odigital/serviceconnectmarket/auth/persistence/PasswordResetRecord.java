package cm.odigital.serviceconnectmarket.auth.persistence;

import java.time.Instant;

public record PasswordResetRecord(
    long utilisateurId,
    Instant expiresAt,
    Instant usedAt,
    String utilisateurStatus
) {
}
