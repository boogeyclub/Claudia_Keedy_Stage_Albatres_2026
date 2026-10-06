package cm.odigital.serviceconnectmarket.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminTableRepository;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminUtilisateurRecord;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.auth.domain.TemporaryPasswordGenerator;
import cm.odigital.serviceconnectmarket.auth.domain.UtilisateurStatus;
import cm.odigital.serviceconnectmarket.auth.messaging.CredentialsMessage;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationMessagingService;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionRepository;

/**
 * Administrator-driven password reset.
 *
 * The administrator never sees or chooses the password: the service generates one, stores only its
 * BCrypt hash, disconnects every browser session of that account, invalidates any pending
 * self-service reset link, and sends the new credentials to the account owner by email. When SMTP
 * is unavailable the whole operation is refused, so the account is never left with a password
 * nobody received.
 */
@Service
public class AdminPasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPasswordResetService.class);

    private final AdminTableRepository adminTableRepository;
    private final AuthRepository authRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final RegistrationMessagingService messagingService;
    private final Clock clock;

    public AdminPasswordResetService(
        AdminTableRepository adminTableRepository,
        AuthRepository authRepository,
        UserSessionRepository userSessionRepository,
        PasswordEncoder passwordEncoder,
        TemporaryPasswordGenerator temporaryPasswordGenerator,
        RegistrationMessagingService messagingService,
        Clock authenticationClock
    ) {
        this.adminTableRepository = adminTableRepository;
        this.authRepository = authRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.messagingService = messagingService;
        this.clock = authenticationClock;
    }

    @Transactional
    public AdminUtilisateurRecord resetPassword(long utilisateurId, long administratorId) {
        LOGGER.info(
            "event=admin-password-reset.workflow.started administratorId={} utilisateurId={}",
            administratorId,
            utilisateurId
        );

        AdminUtilisateurRecord utilisateur = adminTableRepository.findUtilisateurForPasswordReset(utilisateurId)
            .orElseThrow(() -> AuthException.badRequest(
                "ADMIN_PASSWORD_RESET_USER_NOT_FOUND",
                "The account to reset does not exist."
            ));

        if (!UtilisateurStatus.ACTIVE.databaseValue().equals(utilisateur.statut())) {
            LOGGER.warn(
                "event=admin-password-reset.rejected reason=ACCOUNT_NOT_ACTIVE administratorId={} utilisateurId={} statut={}",
                administratorId,
                utilisateurId,
                utilisateur.statut()
            );
            throw AuthException.conflict(
                "ADMIN_PASSWORD_RESET_ACCOUNT_NOT_ACTIVE",
                "Only an active account can receive new credentials."
            );
        }

        Instant now = clock.instant();
        String temporaryPassword = temporaryPasswordGenerator.generate();

        // The mail is sent before the transaction commits anything else: an unavailable SMTP
        // configuration must abort the whole reset rather than change a password silently.
        LOGGER.info("event=admin-password-reset.mail.dispatch.started utilisateurId={}", utilisateurId);
        messagingService.sendCredentials(new CredentialsMessage(
            utilisateur.email(),
            utilisateur.prenom(),
            utilisateur.login(),
            temporaryPassword,
            RegistrationLanguage.FR
        ));
        LOGGER.info("event=admin-password-reset.mail.dispatch.completed utilisateurId={}", utilisateurId);

        authRepository.insertPasswordHash(utilisateurId, passwordEncoder.encode(temporaryPassword), now);
        authRepository.markPasswordResetUsed(utilisateurId, now);
        int invalidatedSessionCount = userSessionRepository.revokeAllForUtilisateur(utilisateurId, now);

        LOGGER.info(
            "event=admin-password-reset.completed administratorId={} utilisateurId={} invalidatedSessionCount={}",
            administratorId,
            utilisateurId,
            invalidatedSessionCount
        );
        return utilisateur;
    }
}
