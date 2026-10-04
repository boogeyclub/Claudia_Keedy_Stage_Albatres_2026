package cm.odigital.serviceconnectmarket.auth.domain;

import java.util.Locale;

/**
 * The legal capacity of a CLIENT account that is registering to source cocoa. VENDEUR accounts do
 * not have a client profile.
 */
public enum ClientProfileType {
    PARTICULIER,
    ENTREPRISE;

    public static ClientProfileType from(String value) {
        if (value == null) {
            throw invalidProfile();
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalidProfile();
        }
    }

    private static AuthException invalidProfile() {
        return AuthException.badRequest(
            "REGISTRATION_CLIENT_PROFILE_INVALID",
            "A CLIENT registration must identify whether the buyer is a private individual or an enterprise."
        );
    }
}
