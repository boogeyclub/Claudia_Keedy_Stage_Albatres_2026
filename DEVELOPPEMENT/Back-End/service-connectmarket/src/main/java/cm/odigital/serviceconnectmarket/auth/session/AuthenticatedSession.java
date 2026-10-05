package cm.odigital.serviceconnectmarket.auth.session;

import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;

/**
 * A verified servlet session paired with its persistent browser-session record.
 */
public record AuthenticatedSession(
    long sessionId,
    AuthenticatedUtilisateur utilisateur
) {
}
