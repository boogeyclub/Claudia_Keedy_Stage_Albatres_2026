package cm.odigital.serviceconnectmarket.auth.api.dto;

public record AuthenticatedUserResponse(
    long id,
    String email,
    String login,
    String prenom,
    String nom,
    String role
) {
}
