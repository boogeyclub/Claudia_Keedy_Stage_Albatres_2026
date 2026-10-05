package cm.odigital.serviceconnectmarket.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import cm.odigital.serviceconnectmarket.auth.config.RegistrationProperties;
import cm.odigital.serviceconnectmarket.auth.domain.ConfirmationTokenGenerator;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationConfirmationMessage;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationMessagingService;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;

/**
 * Issues and sends the single-use e-mail confirmation of a pending account.
 *
 * <p>Both account-creation entry points use it: the public registration workflow and the
 * administrator console. An account created by an administrator therefore follows exactly the same
 * rule as a self-registration — the owner must open the link and confirm before signing in, because
 * {@code AuthenticationService} refuses a {@code EN_ATTENTE_CONFIRMATION} account.
 *
 * <p>The confirmation row and the e-mail are handled in the caller's transaction. When the SMTP
 * delivery fails, the exception propagates and the transaction rolls back, so no account is left in
 * a state that nobody can activate.
 */
@Service
public class RegistrationConfirmationDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationConfirmationDispatcher.class);

    /** A confirmation link stays valid for three hours, for both entry points. */
    public static final Duration CONFIRMATION_TTL = Duration.ofHours(3);

    private final AuthRepository authRepository;
    private final ConfirmationTokenGenerator tokenGenerator;
    private final RegistrationMessagingService messagingService;
    private final RegistrationProperties registrationProperties;
    private final Clock clock;

    public RegistrationConfirmationDispatcher(
        AuthRepository authRepository,
        ConfirmationTokenGenerator tokenGenerator,
        RegistrationMessagingService messagingService,
        RegistrationProperties registrationProperties,
        Clock authenticationClock
    ) {
        this.authRepository = authRepository;
        this.tokenGenerator = tokenGenerator;
        this.messagingService = messagingService;
        this.registrationProperties = registrationProperties;
        this.clock = authenticationClock;
    }

    /**
     * Hashes a presented confirmation token so it can be matched against the stored hash. The token
     * algorithm stays owned by the confirmation dispatcher.
     */
    public String hashToken(String rawToken) {
        return tokenGenerator.hash(rawToken);
    }

    /**
     * Stores the hashed token of the account and sends the confirmation link.
     *
     * @param utilisateurId the pending account identifier
     * @param email the recipient address
     * @param recipientFirstName used by the e-mail greeting
     * @param language the e-mail language
     * @return the instant the link stops being valid
     */
    public Instant dispatch(long utilisateurId, String email, String recipientFirstName, RegistrationLanguage language) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(CONFIRMATION_TTL);
        String rawToken = tokenGenerator.generate();

        authRepository.insertConfirmation(utilisateurId, tokenGenerator.hash(rawToken), expiresAt, now);
        LOGGER.info(
            "event=registration.confirmation.dispatched utilisateurId={} expiresAt={}",
            utilisateurId,
            expiresAt
        );

        LOGGER.info("event=registration.mail.dispatch.started utilisateurId={}", utilisateurId);
        messagingService.sendConfirmation(new RegistrationConfirmationMessage(
            email,
            recipientFirstName,
            confirmationUrl(rawToken),
            expiresAt,
            language
        ));
        LOGGER.info("event=registration.mail.dispatch.completed utilisateurId={}", utilisateurId);

        return expiresAt;
    }

    private String confirmationUrl(String rawToken) {
        return UriComponentsBuilder.fromUriString(registrationProperties.getConfirmationUrl())
            .queryParam("token", rawToken)
            .build()
            .encode()
            .toUriString();
    }
}
