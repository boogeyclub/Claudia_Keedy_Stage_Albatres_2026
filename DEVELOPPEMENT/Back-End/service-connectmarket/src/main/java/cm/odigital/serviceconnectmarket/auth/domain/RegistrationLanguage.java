package cm.odigital.serviceconnectmarket.auth.domain;

import java.util.Locale;

public enum RegistrationLanguage {
    EN,
    FR;

    public static RegistrationLanguage fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return EN;
        }

        return "fr".equals(value.trim().toLowerCase(Locale.ROOT)) ? FR : EN;
    }
}
