package cm.odigital.serviceconnectmarket.auth.api;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cm.odigital.serviceconnectmarket.auth.api.dto.AuthenticatedUserResponse;
import cm.odigital.serviceconnectmarket.auth.api.dto.BrowserSessionResponse;
import cm.odigital.serviceconnectmarket.auth.api.dto.ConfirmationResponse;
import cm.odigital.serviceconnectmarket.auth.api.dto.LoginRequest;
import cm.odigital.serviceconnectmarket.auth.api.dto.PasswordResetConfirmationRequest;
import cm.odigital.serviceconnectmarket.auth.api.dto.PasswordResetConfirmationResponse;
import cm.odigital.serviceconnectmarket.auth.api.dto.PasswordResetRequest;
import cm.odigital.serviceconnectmarket.auth.api.dto.PasswordResetRequestAcceptedResponse;
import cm.odigital.serviceconnectmarket.auth.api.dto.RegistrationAcceptedResponse;
import cm.odigital.serviceconnectmarket.auth.api.dto.RegistrationRequest;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;
import cm.odigital.serviceconnectmarket.auth.domain.PendingRegistration;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationCommand;
import cm.odigital.serviceconnectmarket.auth.service.AuthenticationService;
import cm.odigital.serviceconnectmarket.auth.service.PasswordResetService;
import cm.odigital.serviceconnectmarket.auth.service.RegistrationService;
import cm.odigital.serviceconnectmarket.auth.session.AuthenticatedSession;
import cm.odigital.serviceconnectmarket.auth.session.BrowserSession;
import cm.odigital.serviceconnectmarket.auth.session.SessionAttributes;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionService;
import cm.odigital.serviceconnectmarket.observability.AuditValue;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    /** @deprecated Prefer {@link SessionAttributes#USER_ID}. */
    @Deprecated(forRemoval = false)
    public static final String SESSION_USER_ID = SessionAttributes.USER_ID;

    /** @deprecated Prefer {@link SessionAttributes#USER_ROLE}. */
    @Deprecated(forRemoval = false)
    public static final String SESSION_USER_ROLE = SessionAttributes.USER_ROLE;

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final UserSessionService userSessionService;

    public AuthController(
        RegistrationService registrationService,
        AuthenticationService authenticationService,
        PasswordResetService passwordResetService,
        UserSessionService userSessionService
    ) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
        this.userSessionService = userSessionService;
    }

    @PostMapping("/registration")
    public ResponseEntity<RegistrationAcceptedResponse> register(@Valid @RequestBody RegistrationRequest request) {
        LOGGER.info(
            "event=registration.request.accepted role={} email={} login={}",
            request.role(),
            AuditValue.maskedEmail(request.email()),
            AuditValue.maskedIdentity(request.login())
        );
        PendingRegistration registration = registrationService.startRegistration(new RegistrationCommand(
            request.role(),
            request.clientProfileType(),
            request.raisonSociale(),
            request.niu(),
            request.rccm(),
            request.prenom(),
            request.nom(),
            request.email(),
            request.login(),
            request.password(),
            request.language()
        ));

        LOGGER.info(
            "event=registration.request.completed email={} expiresAt={}",
            AuditValue.maskedEmail(registration.email()),
            registration.expiresAt()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new RegistrationAcceptedResponse(registration.email(), registration.expiresAt()));
    }

    @GetMapping("/registration/confirm")
    public ConfirmationResponse confirmRegistration(@RequestParam String token) {
        LOGGER.info("event=registration.confirmation.request.accepted");
        registrationService.confirmRegistration(token);
        LOGGER.info("event=registration.confirmation.request.completed");
        return new ConfirmationResponse(
            "CONFIRMED",
            "Your CacaoMarketCM registration is confirmed. You can now sign in."
        );
    }

    /**
     * Always returns a generic accepted response so callers cannot determine whether an email
     * belongs to a confirmed account. The service sends mail only for active accounts.
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetRequestAcceptedResponse> requestPasswordReset(
        @Valid @RequestBody PasswordResetRequest request
    ) {
        LOGGER.info("event=password-reset.request.accepted email={}", AuditValue.maskedEmail(request.email()));
        try {
            passwordResetService.requestPasswordReset(request.email(), request.language());
        } catch (AuthException exception) {
            // The service transaction has already rolled back. Keep the public response generic so
            // a temporary SMTP/configuration failure cannot reveal a confirmed account's existence.
            LOGGER.warn("event=password-reset.request.delivery-not-disclosed errorCode={}", exception.getCode());
        }
        LOGGER.info("event=password-reset.request.completed");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new PasswordResetRequestAcceptedResponse(
            "If a confirmed CacaoMarketCM account uses this email address, a password reset link has been sent."
        ));
    }

    @PostMapping("/password-reset/confirm")
    public PasswordResetConfirmationResponse confirmPasswordReset(
        @Valid @RequestBody PasswordResetConfirmationRequest request
    ) {
        LOGGER.info("event=password-reset.confirmation.request.accepted");
        passwordResetService.completePasswordReset(request.token(), request.password());
        LOGGER.info("event=password-reset.confirmation.request.completed");
        return new PasswordResetConfirmationResponse(
            "RESET",
            "Your password has been reset. Sign in with your new password."
        );
    }

    @PostMapping("/login")
    public AuthenticatedUserResponse login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest
    ) {
        LOGGER.info("event=login.request.accepted identity={}", AuditValue.maskedIdentity(request.identity()));
        AuthenticatedUtilisateur utilisateur = authenticationService.authenticate(request.identity(), request.password());
        establishSession(servletRequest, utilisateur, Boolean.TRUE.equals(request.rememberMe()));

        LOGGER.info(
            "event=login.request.completed utilisateurId={} role={} rememberMe={}",
            utilisateur.id(),
            utilisateur.role(),
            Boolean.TRUE.equals(request.rememberMe())
        );
        return authenticatedUserResponse(utilisateur);
    }

    /**
     * Restores the authenticated profile after a browser refresh. The underlying servlet session
     * must also have an active, non-expired row in gu.sessions_utilisateur.
     */
    @GetMapping("/session")
    public AuthenticatedUserResponse currentSession(HttpServletRequest servletRequest) {
        AuthenticatedSession session = userSessionService.requireAuthenticatedSession(servletRequest.getSession(false));
        LOGGER.info(
            "event=session.current.resolved utilisateurId={} role={} sessionRecordId={}",
            session.utilisateur().id(),
            session.utilisateur().role(),
            session.sessionId()
        );
        return authenticatedUserResponse(session.utilisateur());
    }

    /**
     * Shows the signed-in user the active browser sessions for their own account only.
     */
    @GetMapping("/sessions")
    public List<BrowserSessionResponse> currentUserSessions(HttpServletRequest servletRequest) {
        AuthenticatedSession currentSession = userSessionService.requireAuthenticatedSession(servletRequest.getSession(false));
        List<BrowserSessionResponse> sessions = userSessionService.activeSessionsFor(currentSession).stream()
            .map(session -> browserSessionResponse(session, currentSession.sessionId()))
            .toList();
        LOGGER.info(
            "event=session.list.completed utilisateurId={} activeSessionCount={}",
            currentSession.utilisateur().id(),
            sessions.size()
        );
        return sessions;
    }

    /**
     * Ends one of the signed-in user's sessions. The response is intentionally idempotent so an
     * already-expired or already-disconnected browser session does not expose account information.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> disconnectSession(
        @PathVariable("sessionId") long sessionId,
        HttpServletRequest servletRequest
    ) {
        HttpSession servletSession = servletRequest.getSession(false);
        AuthenticatedSession currentSession = userSessionService.requireAuthenticatedSession(servletSession);
        boolean disconnected = userSessionService.revokeOwnedSession(currentSession, sessionId, servletSession);
        LOGGER.info(
            "event=session.disconnect.completed utilisateurId={} sessionRecordId={} disconnected={}",
            currentSession.utilisateur().id(),
            sessionId,
            disconnected
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        boolean activeSessionFound = session != null;
        boolean persistentSessionRevoked = activeSessionFound && userSessionService.revokeServletSession(session);
        if (activeSessionFound) {
            session.invalidate();
        }
        LOGGER.info(
            "event=logout.request.completed activeSessionFound={} persistentSessionRevoked={}",
            activeSessionFound,
            persistentSessionRevoked
        );
        return ResponseEntity.noContent().build();
    }

    private void establishSession(HttpServletRequest request, AuthenticatedUtilisateur utilisateur, boolean rememberMe) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            boolean priorSessionRevoked = userSessionService.revokeServletSession(existingSession);
            request.changeSessionId();
            LOGGER.info("event=session.login.previous-session-rotated persistentSessionRevoked={}", priorSessionRevoked);
        }

        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(rememberMe ? 60 * 60 * 24 * 7 : 60 * 30);
        session.setAttribute(SessionAttributes.USER_ID, utilisateur.id());
        session.setAttribute(SessionAttributes.USER_ROLE, utilisateur.role());
        long persistentSessionId = userSessionService.recordSuccessfulLogin(
            session,
            utilisateur,
            rememberMe,
            browserLabelFor(request)
        );
        LOGGER.info(
            "event=session.login.persisted utilisateurId={} sessionRecordId={} browser={} rememberMe={}",
            utilisateur.id(),
            persistentSessionId,
            browserLabelFor(request),
            rememberMe
        );
    }

    private AuthenticatedUserResponse authenticatedUserResponse(AuthenticatedUtilisateur utilisateur) {
        return new AuthenticatedUserResponse(
            utilisateur.id(),
            utilisateur.email(),
            utilisateur.login(),
            utilisateur.prenom(),
            utilisateur.nom(),
            utilisateur.role()
        );
    }

    private BrowserSessionResponse browserSessionResponse(BrowserSession session, long currentSessionId) {
        return new BrowserSessionResponse(
            session.id(),
            session.browserLabel(),
            session.rememberMe(),
            session.createdAt(),
            session.lastSeenAt(),
            session.expiresAt(),
            session.id() == currentSessionId
        );
    }

    /**
     * Stores only a compact generic label, never the full User-Agent header, in PostgreSQL or logs.
     */
    private String browserLabelFor(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Browser session";
        }

        String normalized = userAgent.toLowerCase(Locale.ROOT);
        String browser = normalized.contains("edg/") ? "Microsoft Edge"
            : normalized.contains("firefox/") ? "Firefox"
            : normalized.contains("chrome/") || normalized.contains("crios/") ? "Google Chrome"
            : normalized.contains("safari/") ? "Safari"
            : "Browser";
        String platform = normalized.contains("android") ? "Android"
            : normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ipod") ? "iOS"
            : normalized.contains("windows") ? "Windows"
            : normalized.contains("mac os") || normalized.contains("macintosh") ? "macOS"
            : normalized.contains("linux") ? "Linux"
            : "your device";
        return browser + " on " + platform;
    }
}
