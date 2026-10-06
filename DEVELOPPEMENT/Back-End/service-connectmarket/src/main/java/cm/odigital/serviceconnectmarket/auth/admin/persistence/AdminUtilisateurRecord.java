package cm.odigital.serviceconnectmarket.auth.admin.persistence;

/**
 * Minimal, safe view of an account an administrator asked to reset.
 *
 * It carries what the reset needs and nothing more: no password hash, no session identifier.
 */
public record AdminUtilisateurRecord(
    long id,
    String email,
    String prenom,
    String login,
    String statut,
    String typeCode
) {
}
