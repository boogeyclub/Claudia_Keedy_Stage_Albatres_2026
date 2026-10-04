package cm.odigital.serviceconnectmarket.auth.admin.domain;

import java.util.Arrays;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;

/**
 * Whitelisted tables exposed by the administrator console. Their SQL names are never accepted
 * directly from a request, preventing table-name injection in the administrative data API.
 */
public enum AdminTable {
    TYPE_UTILISATEUR("type_utilisateur"),
    UTILISATEURS("utilisateurs"),
    CLIENT_PARTICULIER("client_particulier"),
    CLIENT_ENTREPRISE("client_entreprise"),
    SESSIONS_UTILISATEUR("sessions_utilisateur"),
    REGISTRATION_CONFIRMATION("registration_confirmation"),
    PASSWORD_RESET("password_reset"),
    BASIC_RIGHTS("basic_rights"),
    TYPE_UTILISATEUR_BASIC_RIGHT("type_utilisateur_basic_right"),
    PASSWORD_HISTORY("password_history");

    private final String apiKey;

    AdminTable(String apiKey) {
        this.apiKey = apiKey;
    }

    public String apiKey() {
        return apiKey;
    }

    public static AdminTable fromApiKey(String apiKey) {
        return Arrays.stream(values())
            .filter(table -> table.apiKey.equals(apiKey))
            .findFirst()
            .orElseThrow(() -> AuthException.badRequest(
                "ADMIN_TABLE_UNSUPPORTED",
                "The requested administrative table is not supported."
            ));
    }
}
