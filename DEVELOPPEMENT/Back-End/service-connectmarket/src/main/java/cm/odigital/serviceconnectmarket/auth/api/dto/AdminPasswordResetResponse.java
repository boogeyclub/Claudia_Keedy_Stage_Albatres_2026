package cm.odigital.serviceconnectmarket.auth.api.dto;

/**
 * Confirms that an administrator reset an account.
 *
 * The generated password is deliberately absent: it only travels to the account owner by email.
 */
public record AdminPasswordResetResponse(
    long utilisateurId,
    String login,
    String email,
    String status,
    String message
) {
}
