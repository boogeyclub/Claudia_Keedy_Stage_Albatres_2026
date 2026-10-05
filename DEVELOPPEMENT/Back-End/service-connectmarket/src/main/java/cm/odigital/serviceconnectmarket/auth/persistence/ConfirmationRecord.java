package cm.odigital.serviceconnectmarket.auth.persistence;

import java.time.Instant;

public record ConfirmationRecord(
    long utilisateurId,
    Instant expiresAt,
    Instant confirmedAt,
    String utilisateurStatus
) {
}
