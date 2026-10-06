package cm.odigital.serviceconnectmarket.auth.admin.api;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminUtilisateurRecord;
import cm.odigital.serviceconnectmarket.auth.api.dto.AdminPasswordResetResponse;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.service.AdminPasswordResetService;
import cm.odigital.serviceconnectmarket.auth.session.AuthenticatedSession;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionService;

/**
 * Administrator password reset.
 *
 * Only an administrator can trigger it, the new password is generated server-side, and the account
 * owner receives it by email. The response never contains the password.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminPasswordResetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPasswordResetController.class);
    private static final String ADMINISTRATOR_ROLE = "ADMINISTRATEUR";

    private final UserSessionService userSessionService;
    private final AdminPasswordResetService adminPasswordResetService;

    public AdminPasswordResetController(
        UserSessionService userSessionService,
        AdminPasswordResetService adminPasswordResetService
    ) {
        this.userSessionService = userSessionService;
        this.adminPasswordResetService = adminPasswordResetService;
    }

    @PostMapping("/{utilisateurId}/password-reset")
    public AdminPasswordResetResponse resetPassword(
        @PathVariable long utilisateurId,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession administrator = requireAdministrator(servletRequest);
        AdminUtilisateurRecord utilisateur = adminPasswordResetService.resetPassword(
            utilisateurId,
            administrator.utilisateur().id()
        );
        LOGGER.info(
            "event=admin-password-reset.request.completed administratorId={} utilisateurId={}",
            administrator.utilisateur().id(),
            utilisateurId
        );
        return new AdminPasswordResetResponse(
            utilisateur.id(),
            utilisateur.login(),
            utilisateur.email(),
            "RESET",
            "New credentials were generated and sent to the account owner."
        );
    }

    private AuthenticatedSession requireAdministrator(HttpServletRequest servletRequest) {
        AuthenticatedSession session = userSessionService.requireAuthenticatedSession(servletRequest.getSession(false));
        if (!ADMINISTRATOR_ROLE.equals(session.utilisateur().role())) {
            LOGGER.warn(
                "event=admin-password-reset.access.denied utilisateurId={} role={}",
                session.utilisateur().id(),
                session.utilisateur().role()
            );
            throw AuthException.forbidden(
                "ADMINISTRATOR_ACCESS_REQUIRED",
                "An active administrator session is required for this operation."
            );
        }
        return session;
    }
}
