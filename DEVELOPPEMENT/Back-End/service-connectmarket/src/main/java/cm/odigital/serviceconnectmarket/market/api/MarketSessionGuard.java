package cm.odigital.serviceconnectmarket.market.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.session.AuthenticatedSession;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionService;

/**
 * Resolves the signed-in account for the catalogue and messaging endpoints and applies the role
 * rule of each side: only a VENDEUR publishes lots, only a CLIENT opens a buyer conversation.
 *
 * <p>Authentication is checked again here on every request; the Angular route guard is never
 * treated as authorization.
 */
@Component
public class MarketSessionGuard {

    private static final String CLIENT_ROLE = "CLIENT";
    private static final String VENDEUR_ROLE = "VENDEUR";

    private final UserSessionService userSessionService;

    public MarketSessionGuard(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    public AuthenticatedSession requireSession(HttpServletRequest servletRequest) {
        return userSessionService.requireAuthenticatedSession(servletRequest.getSession(false));
    }

    public AuthenticatedSession requireVendeur(HttpServletRequest servletRequest) {
        AuthenticatedSession session = requireSession(servletRequest);
        if (!VENDEUR_ROLE.equals(session.utilisateur().role())) {
            throw AuthException.forbidden(
                "MARKET_VENDEUR_REQUIRED",
                "An active seller account is required for this operation."
            );
        }
        return session;
    }

    public AuthenticatedSession requireClient(HttpServletRequest servletRequest) {
        AuthenticatedSession session = requireSession(servletRequest);
        if (!CLIENT_ROLE.equals(session.utilisateur().role())) {
            throw AuthException.forbidden(
                "MARKET_CLIENT_REQUIRED",
                "An active buyer account is required for this operation."
            );
        }
        return session;
    }
}
