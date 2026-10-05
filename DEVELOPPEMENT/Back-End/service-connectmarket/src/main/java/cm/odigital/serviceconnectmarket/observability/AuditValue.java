package cm.odigital.serviceconnectmarket.observability;

public final class AuditValue {

    private AuditValue() {
    }

    /**
     * Keeps operational logs useful without writing a complete email address.
     */
    public static String maskedEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[missing]";
        }

        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0 || atIndex == trimmed.length() - 1) {
            return "[invalid-email]";
        }

        return visiblePrefix(trimmed.substring(0, atIndex), 1) + "@" + trimmed.substring(atIndex + 1);
    }

    /**
     * Keeps a login traceable while not printing the whole identifier.
     */
    public static String maskedIdentity(String value) {
        if (value == null || value.isBlank()) {
            return "[missing]";
        }

        return visiblePrefix(value.trim(), 2);
    }

    private static String visiblePrefix(String value, int visibleLength) {
        if (value.length() <= visibleLength) {
            return "*".repeat(value.length());
        }

        return value.substring(0, visibleLength) + "***";
    }
}
