package cm.odigital.serviceconnectmarket.auth.persistence;

/**
 * A confirmed account that is eligible to receive a password-reset message.
 */
public record PasswordResetCandidate(
    long utilisateurId,
    String email,
    String prenom
) {
}
