package cm.odigital.serviceconnectmarket.auth.domain;

public record AuthenticatedUtilisateur(
    long id,
    String email,
    String login,
    String prenom,
    String nom,
    String role
) {
}
