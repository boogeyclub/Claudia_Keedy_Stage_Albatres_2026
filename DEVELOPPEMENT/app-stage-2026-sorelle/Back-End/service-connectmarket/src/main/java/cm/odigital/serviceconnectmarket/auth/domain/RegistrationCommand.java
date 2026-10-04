package cm.odigital.serviceconnectmarket.auth.domain;

public record RegistrationCommand(
    String role,
    String clientProfileType,
    String raisonSociale,
    String niu,
    String rccm,
    String prenom,
    String nom,
    String email,
    String login,
    String password,
    String language
) {
}
