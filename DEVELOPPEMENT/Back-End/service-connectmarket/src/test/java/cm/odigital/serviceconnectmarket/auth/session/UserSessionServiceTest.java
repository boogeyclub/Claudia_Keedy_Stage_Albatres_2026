package cm.odigital.serviceconnectmarket.auth.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private SessionIdHasher sessionIdHasher;

    private UserSessionService userSessionService;

    @BeforeEach
    void setUp() {
        userSessionService = new UserSessionService(
            sessionRepository,
            sessionIdHasher,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void recordsOnePersistentSessionForASuccessfulBrowserLogin() {
        MockHttpSession servletSession = new MockHttpSession();
        servletSession.setMaxInactiveInterval(1_800);
        AuthenticatedUtilisateur utilisateur = utilisateur();
        when(sessionIdHasher.hash(servletSession.getId())).thenReturn("hashed-servlet-session-id");
        when(sessionRepository.create(
            eq(7L),
            eq("hashed-servlet-session-id"),
            eq("Google Chrome on Windows"),
            eq(false),
            eq(NOW),
            eq(NOW.plusSeconds(1_800))
        )).thenReturn(31L);

        long persistentSessionId = userSessionService.recordSuccessfulLogin(
            servletSession,
            utilisateur,
            false,
            "Google Chrome on Windows"
        );

        assertEquals(31L, persistentSessionId);
        assertEquals(31L, servletSession.getAttribute(SessionAttributes.PERSISTED_SESSION_ID));
    }

    @Test
    void acceptsOnlyAValidDatabaseSessionThatMatchesTheServletProfile() {
        MockHttpSession servletSession = new MockHttpSession();
        servletSession.setMaxInactiveInterval(1_800);
        servletSession.setAttribute(SessionAttributes.USER_ID, 7L);
        servletSession.setAttribute(SessionAttributes.USER_ROLE, "CLIENT");
        servletSession.setAttribute(SessionAttributes.PERSISTED_SESSION_ID, 31L);
        when(sessionIdHasher.hash(servletSession.getId())).thenReturn("hashed-servlet-session-id");
        when(sessionRepository.findActiveBySessionHash("hashed-servlet-session-id", NOW)).thenReturn(Optional.of(
            new AuthenticatedSession(31L, utilisateur())
        ));
        when(sessionRepository.touch(31L, NOW, NOW.plusSeconds(1_800))).thenReturn(true);

        AuthenticatedSession session = userSessionService.requireAuthenticatedSession(servletSession);

        assertEquals(31L, session.sessionId());
        assertEquals("CLIENT", session.utilisateur().role());
        verify(sessionRepository).touch(31L, NOW, NOW.plusSeconds(1_800));
    }

    @Test
    void invalidatesTheCurrentServletSessionWhenTheUserDisconnectsThatBrowser() {
        MockHttpSession servletSession = new MockHttpSession();
        AuthenticatedSession currentSession = new AuthenticatedSession(31L, utilisateur());
        when(sessionRepository.revokeOwnedSession(31L, 7L, NOW)).thenReturn(true);

        boolean revoked = userSessionService.revokeOwnedSession(currentSession, 31L, servletSession);

        assertTrue(revoked);
        assertTrue(servletSession.isInvalid());
    }

    private AuthenticatedUtilisateur utilisateur() {
        return new AuthenticatedUtilisateur(
            7L,
            "buyer@example.com",
            "buyer-team",
            "Noah",
            "Buyer",
            "CLIENT"
        );
    }
}
