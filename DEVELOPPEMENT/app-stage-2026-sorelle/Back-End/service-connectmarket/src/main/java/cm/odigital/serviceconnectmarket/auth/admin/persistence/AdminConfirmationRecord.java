package cm.odigital.serviceconnectmarket.auth.admin.persistence;

import java.time.Instant;

/**
 * Administrative confirmation metadata. The raw or hashed confirmation token is never exposed.
 */
public record AdminConfirmationRecord(long utilisateurId, String utilisateurStatus, Instant confirmedAt) {
}
