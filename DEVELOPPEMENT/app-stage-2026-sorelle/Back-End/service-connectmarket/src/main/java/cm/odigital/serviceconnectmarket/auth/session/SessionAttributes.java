package cm.odigital.serviceconnectmarket.auth.session;

/**
 * Names used inside the server-side HttpSession. These values never leave the server.
 */
public final class SessionAttributes {

    public static final String USER_ID = "cacaomarket.auth.user-id";
    public static final String USER_ROLE = "cacaomarket.auth.user-role";
    public static final String PERSISTED_SESSION_ID = "cacaomarket.auth.browser-session-id";

    private SessionAttributes() {
    }
}
