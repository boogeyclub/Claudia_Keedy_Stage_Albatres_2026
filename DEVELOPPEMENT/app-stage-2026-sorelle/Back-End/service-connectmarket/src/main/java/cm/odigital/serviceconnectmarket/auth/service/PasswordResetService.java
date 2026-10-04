package cm.odigital.serviceconnectmarket.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import cm.odigital.serviceconnectmarket.auth.config.PasswordResetProperties;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.ConfirmationTokenGenerator;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.auth.domain.UtilisateurStatus;
import cm.odigital.serviceconnectmarket.auth.messaging.PasswordResetMessage;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationMessagingService;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.persistence.PasswordResetCandidate;
import cm.odigital.serviceconnectmarket.auth.persistence.PasswordResetRecord;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionRepository;
import cm.odigital.serviceconnectmarket.observability.AuditValue;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int BCRYPT_MAXIMUM_BYTES = 72;

    private final AuthRepository authRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfirmationTokenGenerator tokenGenerator;
    private final RegistrationMessagingService messagingService;
    private final PasswordResetProperties passwordResetProperties;
    private final Clock clock;

    public PasswordResetService(
        AuthRepository authRepository,
        UserSessionRepository userSessionRepository,
        PasswordEncoder passwordEncoder,
        ConfirmationTokenGenerator tokenGenerator,
        RegistrationMessagingService messagingService,
        PasswordResetProperties passwordResetProperties,
        Clock authenticationClock
    ) {
        this.authRepository = authRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.messagingService = messagingService;
        this.passwordResetProperties = passwordResetProperties;
        this.clock = authenticationClock;
    }

    /**
     * Sends a reset link only when the submitted address belongs to a confirmed/active account.
     * Callers intentionally receive the same accepted response whether or not a matching account
     * exists, so this operation does not expose account-confirmation state.
     */
    @Transactional
    public void requestPasswordReset(String submittedEmail, String language) {
        String email = normalizeEmail(submittedEmail);
        LOGGER.info("event=password-reset.request.workflow.started email={}", AuditValue.maskedEmail(email));

        PasswordResetCandidate candidate = authRepository.findActiveUtilisateurByEmail(email).orElse(null);
        if (candidate == null) {
            LOGGER.info("event=password-reset.request.not-eligible");
            return;
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(tokenTtl());
        String rawToken = tokenGenerator.generate();
        authRepository.upsertPasswordReset(candidate.utilisateurId(), tokenGenerator.hash(rawToken), expiresAt, now);
        LOGGER.info(
            "event=password-reset.request.persisted utilisateurId={} expiresAt={}",
            candidate.utilisateurId(),
            expiresAt
        );

        LOGGER.info("event=password-reset.mail.dispatch.started utilisateurId={}", candidate.utilisateurId());
        messagingService.sendPasswordReset(new PasswordResetMessage(
            candidate.email(),
            candidate.prenom(),
            resetUrl(rawToken),
            expiresAt,
            RegistrationLanguage.fromNullable(language)
        ));
        LOGGER.info("event=password-reset.mail.dispatch.completed utilisateurId={}", candidate.utilisateurId());
    }

    /**
     * Replaces the password after consuming one valid token and disconnects every browser session
     * belonging to that account, including the browser that requested the reset.
     */
    @Transactional
    public void completePasswordReset(String rawToken, String password) {
        LOGGER.info("event=password-reset.confirmation.workflow.started");
        if (rawToken == null || rawToken.isBlank()) {
            throw AuthException.badRequest("PASSWORD_RESET_TOKEN_MISSING", "A password reset token is required.");
        }
        validatePasswordLength(password);

        PasswordResetRecord reset = authRepository.findPasswordResetByTokenHash(tokenGenerator.hash(rawToken))
            .orElseThrow(() -> AuthException.badRequest(
                "PASSWORD_RESET_TOKEN_INVALID",
                "This password reset link is invalid."
            ));
        LOGGER.info("event=password-reset.confirmation.record-found utilisateurId={}", reset.utilisateurId());

        if (reset.usedAt() != null) {
            throw AuthException.conflict(
                "PASSWORD_RESET_TOKEN_ALREADY_USED",
                "This password reset link has already been used."
            );
        }

        Instant now = clock.instant();
        if (!now.isBefore(reset.expiresAt())) {
            LOGGER.warn("event=password-reset.confirmation.expired utilisateurId={}", reset.utilisateurId());
            throw AuthException.gone(
                "PASSWORD_RESET_TOKEN_EXPIRED",
                "This password reset link has expired. Request a new link."
            );
        }

        if (!UtilisateurStatus.ACTIVE.databaseValue().equals(reset.utilisateurStatus())) {
            LOGGER.warn("event=password-reset.confirmation.rejected reason=ACCOUNT_NOT_ACTIVE utilisateurId={}", reset.utilisateurId());
            throw AuthException.conflict(
                "PASSWORD_RESET_ACCOUNT_UNAVAILABLE",
                "This account is not available for password reset."
            );
        }

        authRepository.insertPasswordHash(reset.utilisateurId(), passwordEncoder.encode(password), now);
        authRepository.markPasswordResetUsed(reset.utilisateurId(), now);
        int invalidatedSessionCount = userSessionRepository.revokeAllForUtilisateur(reset.utilisateurId(), now);
        LOGGER.info(
            "event=password-reset.confirmation.completed utilisateurId={} invalidatedSessionCount={}",
            reset.utilisateurId(),
            invalidatedSessionCount
        );
    }

    private Duration tokenTtl() {
        Duration configuredTtl = passwordResetProperties.getTokenTtl();
        if (configuredTtl == null || configuredTtl.isZero() || configuredTtl.isNegative()) {
            throw AuthException.unavailable(
                "PASSWORD_RESET_CONFIGURATION_INVALID",
                "The password reset expiry configuration is invalid."
            );
        }
        return configuredTtl;
    }

    private String resetUrl(String rawToken) {
        String configuredResetUrl = passwordResetProperties.getResetUrl();
        if (configuredResetUrl == null || configuredResetUrl.isBlank()) {
            throw AuthException.unavailable(
                "PASSWORD_RESET_CONFIGURATION_INVALID",
                "The password reset URL configuration is invalid."
            );
        }

        try {
            return UriComponentsBuilder.fromUriString(configuredResetUrl)
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
        } catch (IllegalArgumentException exception) {
            throw AuthException.unavailable(
                "PASSWORD_RESET_CONFIGURATION_INVALID",
                "The password reset URL configuration is invalid."
            );
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw AuthException.badRequest("PASSWORD_RESET_EMAIL_INVALID", "An email address is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_BYTES) {
            throw AuthException.badRequest(
                "PASSWORD_RESET_PASSWORD_INVALID",
                "The password does not meet the supported length requirements."
            );
        }
    }
}
