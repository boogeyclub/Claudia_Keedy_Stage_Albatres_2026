package cm.odigital.serviceconnectmarket.auth.session;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;

@Service
public class UserSessionService {

    private static final int FALLBACK_SESSION_TIMEOUT_SECONDS = 30 * 60;

    private final UserSessionRepository sessionRepository;
    private final SessionIdHasher sessionIdHasher;
    private final Clock clock;

    public UserSessionService(
        UserSessionRepository sessionRepository,
        SessionIdHasher sessionIdHasher,
        Clock authenticationClock
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionIdHasher = sessionIdHasher;
        this.clock = authenticationClock;
    }

    /**
     * Stores the successful login as a distinct browser session. A user can have multiple
     * simultaneous rows, one for each browser/device cookie.
     */
    public long recordSuccessfulLogin(
        HttpSession servletSession,
        AuthenticatedUtilisateur utilisateur,
        boolean rememberMe,
        String browserLabel
    ) {
        Instant now = clock.instant();
        long sessionId = sessionRepository.create(
            utilisateur.id(),
            sessionIdHasher.hash(servletSession.getId()),
            browserLabel,
            rememberMe,
            now,
            expirationFor(servletSession, now)
        );
        servletSession.setAttribute(SessionAttributes.PERSISTED_SESSION_ID, sessionId);
        return sessionId;
    }

    /**
     * Validates both the servlet cookie and its active database record before allowing access.
     */
    public AuthenticatedSession requireAuthenticatedSession(HttpSession servletSession) {
        if (servletSession == null) {
            throw notAuthenticated();
        }

        Object storedUserId = servletSession.getAttribute(SessionAttributes.USER_ID);
        Object storedRole = servletSession.getAttribute(SessionAttributes.USER_ROLE);
        Object storedPersistentSessionId = servletSession.getAttribute(SessionAttributes.PERSISTED_SESSION_ID);
        if (!(storedUserId instanceof Long userId)
            || !(storedRole instanceof String userRole)
            || !(storedPersistentSessionId instanceof Long persistentSessionId)) {
            invalidate(servletSession);
            throw notAuthenticated();
        }

        Instant now = clock.instant();
        AuthenticatedSession session = sessionRepository.findActiveBySessionHash(
            sessionIdHasher.hash(servletSession.getId()),
            now
        ).orElseGet(() -> {
            invalidate(servletSession);
            throw notAuthenticated();
        });

        AuthenticatedUtilisateur utilisateur = session.utilisateur();
        if (session.sessionId() != persistentSessionId
            || utilisateur.id() != userId
            || !utilisateur.role().equals(userRole)) {
            invalidate(servletSession);
            throw notAuthenticated();
        }

        if (!sessionRepository.touch(session.sessionId(), now, expirationFor(servletSession, now))) {
            invalidate(servletSession);
            throw notAuthenticated();
        }

        return session;
    }

    public List<BrowserSession> activeSessionsFor(AuthenticatedSession session) {
        return sessionRepository.findActiveForUtilisateur(session.utilisateur().id(), clock.instant());
    }

    /**
     * Ends a session only when it belongs to the authenticated user. Ending the current row also
     * invalidates its servlet session, so the browser is immediately signed out.
     */
    public boolean revokeOwnedSession(
        AuthenticatedSession currentSession,
        long sessionId,
        HttpSession servletSession
    ) {
        boolean revoked = sessionRepository.revokeOwnedSession(
            sessionId,
            currentSession.utilisateur().id(),
            clock.instant()
        );
        if (revoked && currentSession.sessionId() == sessionId) {
            servletSession.invalidate();
        }
        return revoked;
    }

    /**
     * Used on logout and before a session ID is rotated during a new login in the same browser.
     */
    public boolean revokeServletSession(HttpSession servletSession) {
        if (servletSession == null) {
            return false;
        }
        return sessionRepository.revokeBySessionHash(sessionIdHasher.hash(servletSession.getId()), clock.instant());
    }

    public int invalidateExpiredSessions() {
        return sessionRepository.invalidateExpiredSessions(clock.instant());
    }

    private void invalidate(HttpSession servletSession) {
        revokeServletSession(servletSession);
        servletSession.invalidate();
    }

    private Instant expirationFor(HttpSession servletSession, Instant now) {
        int timeoutSeconds = servletSession.getMaxInactiveInterval();
        return now.plusSeconds(timeoutSeconds > 0 ? timeoutSeconds : FALLBACK_SESSION_TIMEOUT_SECONDS);
    }

    private AuthException notAuthenticated() {
        return AuthException.unauthorized(
            "SESSION_NOT_AUTHENTICATED",
            "An active CacaoMarketCM session is required."
        );
    }
}
