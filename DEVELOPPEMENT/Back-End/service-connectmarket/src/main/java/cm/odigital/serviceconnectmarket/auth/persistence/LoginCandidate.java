package cm.odigital.serviceconnectmarket.auth.persistence;

public record LoginCandidate(
    long id,
    String email,
    String login,
    String prenom,
    String nom,
    String userType,
    String statut,
    String passwordHash
) {
}
