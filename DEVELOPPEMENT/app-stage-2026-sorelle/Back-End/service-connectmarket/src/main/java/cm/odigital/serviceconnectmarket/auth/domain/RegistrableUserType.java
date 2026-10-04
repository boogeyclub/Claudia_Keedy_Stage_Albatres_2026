package cm.odigital.serviceconnectmarket.auth.domain;

import java.util.Locale;

public enum RegistrableUserType {
    VENDEUR,
    CLIENT;

    public static RegistrableUserType from(String value) {
        if (value == null) {
            throw AuthException.badRequest("REGISTRATION_TYPE_INVALID", "Only CLIENT and VENDEUR registrations are supported.");
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw AuthException.badRequest("REGISTRATION_TYPE_INVALID", "Only CLIENT and VENDEUR registrations are supported.");
        }
    }
}
