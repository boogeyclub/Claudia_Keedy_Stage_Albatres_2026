package cm.odigital.serviceconnectmarket.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionRepository;

/**
 * Lets a signed-in account change its own password from the account settings.
 *
 * The current password must be supplied and verified, so a stolen browser session alone cannot lock
 * the owner out. The new password replaces the stored hash (the database trigger archives the
 * previous one) and every other browser session is disconnected; the session that performed the
 * change stays valid.
 */
@Service
public class AccountPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountPasswordService.class);
    private static final int MINIMUM_LENGTH = 8;
    private static final int BCRYPT_MAXIMUM_BYTES = 72;

    private final AuthRepository authRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountPasswordService(
        AuthRepository authRepository,
        UserSessionRepository userSessionRepository,
        PasswordEncoder passwordEncoder,
        Clock authenticationClock
    ) {
        this.authRepository = authRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = authenticationClock;
    }

    @Transactional
    public void changePassword(long utilisateurId, long keptSessionId, String currentPassword, String newPassword) {
        LOGGER.info("event=account-password.change.workflow.started utilisateurId={}", utilisateurId);

        if (currentPassword == null || currentPassword.isEmpty()) {
            throw AuthException.badRequest(
                "ACCOUNT_PASSWORD_CURRENT_REQUIRED",
                "Your current password is required to change it."
            );
        }
        validateNewPassword(newPassword);

        String currentHash = authRepository.findCurrentPasswordHash(utilisateurId)
            .orElseThrow(() -> {
                LOGGER.warn(
                    "event=account-password.change.rejected reason=NO_CURRENT_PASSWORD utilisateurId={}",
                    utilisateurId
                );
                return AuthException.conflict(
                    "ACCOUNT_PASSWORD_UNAVAILABLE",
                    "This account has no usable password yet."
                );
            });

        if (!passwordEncoder.matches(currentPassword, currentHash)) {
            LOGGER.warn(
                "event=account-password.change.rejected reason=CURRENT_PASSWORD_MISMATCH utilisateurId={}",
                utilisateurId
            );
            throw AuthException.badRequest(
                "ACCOUNT_PASSWORD_CURRENT_INVALID",
                "The current password is incorrect."
            );
        }

        if (passwordEncoder.matches(newPassword, currentHash)) {
            throw AuthException.badRequest(
                "ACCOUNT_PASSWORD_UNCHANGED",
                "Choose a password different from the current one."
            );
        }

        Instant now = clock.instant();
        authRepository.insertPasswordHash(utilisateurId, passwordEncoder.encode(newPassword), now);
        int invalidatedSessionCount = userSessionRepository.revokeAllForUtilisateurExcept(
            utilisateurId,
            keptSessionId,
            now
        );
        LOGGER.info(
            "event=account-password.change.completed utilisateurId={} invalidatedSessionCount={}",
            utilisateurId,
            invalidatedSessionCount
        );
    }

    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < MINIMUM_LENGTH) {
            throw AuthException.badRequest(
                "ACCOUNT_PASSWORD_TOO_SHORT",
                "The new password must contain at least " + MINIMUM_LENGTH + " characters."
            );
        }
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_BYTES) {
            throw AuthException.badRequest(
                "ACCOUNT_PASSWORD_TOO_LONG",
                "The new password is longer than the supported maximum."
            );
        }
    }
}
