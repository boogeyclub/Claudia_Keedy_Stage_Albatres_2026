package cm.odigital.serviceconnectmarket.auth.admin.persistence;

/**
 * Internal administrative snapshot. Password hashes are deliberately absent.
 */
public record AdminUserRecord(
    long id,
    long typeUtilisateurId,
    String typeCode,
    String nom,
    String prenom,
    String email,
    String login,
    String statut
) {
}
