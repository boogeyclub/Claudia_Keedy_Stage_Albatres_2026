package cm.odigital.serviceconnectmarket.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import cm.odigital.serviceconnectmarket.auth.config.RegistrationProperties;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.ClientProfileType;
import cm.odigital.serviceconnectmarket.auth.domain.ConfirmationTokenGenerator;
import cm.odigital.serviceconnectmarket.auth.domain.PendingRegistration;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrableUserType;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationCommand;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.auth.domain.UtilisateurStatus;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationConfirmationMessage;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationMessagingService;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.persistence.ConfirmationRecord;
import cm.odigital.serviceconnectmarket.observability.AuditValue;

@Service
public class RegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);
    private static final Duration CONFIRMATION_TTL = Duration.ofHours(3);
    private static final int BCRYPT_MAXIMUM_BYTES = 72;

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfirmationTokenGenerator tokenGenerator;
    private final RegistrationMessagingService messagingService;
    private final RegistrationExpiryService registrationExpiryService;
    private final RegistrationProperties registrationProperties;
    private final Clock clock;

    public RegistrationService(
        AuthRepository authRepository,
        PasswordEncoder passwordEncoder,
        ConfirmationTokenGenerator tokenGenerator,
        RegistrationMessagingService messagingService,
        RegistrationExpiryService registrationExpiryService,
        RegistrationProperties registrationProperties,
        Clock authenticationClock
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.messagingService = messagingService;
        this.registrationExpiryService = registrationExpiryService;
        this.registrationProperties = registrationProperties;
        this.clock = authenticationClock;
    }

    @Transactional
    public PendingRegistration startRegistration(RegistrationCommand command) {
        registrationExpiryService.removeExpiredRegistrations();
        Instant now = clock.instant();

        RegistrableUserType userType = RegistrableUserType.from(command.role());
        ClientProfile profile = clientProfileFor(userType, command);
        String email = normalizeEmail(command.email());
        String login = normalizeRequired(command.login());
        String prenom = normalizeRequired(command.prenom());
        String nom = normalizeRequired(command.nom());
        validatePasswordLength(command.password());

        LOGGER.info(
            "event=registration.workflow.started role={} clientProfileType={} email={} login={}",
            userType.name(),
            profile == null ? "[none]" : profile.type().name(),
            AuditValue.maskedEmail(email),
            AuditValue.maskedIdentity(login)
        );
        if (authRepository.identityExists(email, login)) {
            LOGGER.warn("event=registration.workflow.rejected reason=IDENTITY_ALREADY_EXISTS");
            throw AuthException.conflict(
                "REGISTRATION_IDENTITY_ALREADY_EXISTS",
                "An account already uses this email address or login."
            );
        }
        LOGGER.info("event=registration.identity.available");
        if (profile != null && profile.type() == ClientProfileType.ENTREPRISE
            && authRepository.enterpriseIdentifiersExist(profile.niu(), profile.rccm())) {
            LOGGER.warn("event=registration.workflow.rejected reason=ENTERPRISE_IDENTIFIER_ALREADY_EXISTS");
            throw AuthException.conflict(
                "REGISTRATION_ENTERPRISE_IDENTIFIER_ALREADY_EXISTS",
                "An enterprise account already uses one of these registered identifiers."
            );
        }

        long typeUtilisateurId = authRepository.findUserTypeId(userType.name())
            .orElseThrow(() -> AuthException.unavailable(
                "REGISTRATION_TYPE_CONFIGURATION_MISSING",
                "The requested user type is not configured."
            ));
        LOGGER.info("event=registration.user-type.resolved role={} typeUtilisateurId={}", userType.name(), typeUtilisateurId);

        String rawToken = tokenGenerator.generate();
        Instant expiresAt = now.plus(CONFIRMATION_TTL);

        try {
            long utilisateurId = authRepository.insertUtilisateur(
                typeUtilisateurId,
                nom,
                prenom,
                email,
                login,
                UtilisateurStatus.PENDING_CONFIRMATION.databaseValue(),
                now
            );
            insertClientProfile(utilisateurId, profile);
            authRepository.insertPasswordHash(utilisateurId, passwordEncoder.encode(command.password()), now);
            authRepository.insertConfirmation(utilisateurId, tokenGenerator.hash(rawToken), expiresAt, now);
            LOGGER.info(
                "event=registration.pending.persisted utilisateurId={} expiresAt={}",
                utilisateurId,
                expiresAt
            );

            LOGGER.info("event=registration.mail.dispatch.started utilisateurId={}", utilisateurId);
            messagingService.sendConfirmation(new RegistrationConfirmationMessage(
                email,
                prenom,
                confirmationUrl(rawToken),
                expiresAt,
                RegistrationLanguage.fromNullable(command.language())
            ));
            LOGGER.info("event=registration.mail.dispatch.completed utilisateurId={}", utilisateurId);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent request can race the identity/enterprise-ID checks above. Keep the
            // response generic and never log the submitted NIU, RCCM, or any database message.
            LOGGER.warn("event=registration.workflow.rejected reason=DATA_INTEGRITY_VIOLATION");
            throw AuthException.conflict(
                "REGISTRATION_IDENTITY_OR_ENTERPRISE_IDENTIFIER_EXISTS",
                "An account or enterprise identifier is already registered."
            );
        }

        LOGGER.info("event=registration.workflow.completed expiresAt={}", expiresAt);
        return new PendingRegistration(email, expiresAt);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public void confirmRegistration(String rawToken) {
        LOGGER.info("event=registration.confirmation.workflow.started");
        if (rawToken == null || rawToken.isBlank()) {
            throw AuthException.badRequest("REGISTRATION_CONFIRMATION_TOKEN_MISSING", "A confirmation token is required.");
        }

        ConfirmationRecord confirmation = authRepository.findConfirmationByTokenHash(tokenGenerator.hash(rawToken))
            .orElseThrow(() -> AuthException.badRequest(
                "REGISTRATION_CONFIRMATION_TOKEN_INVALID",
                "This registration confirmation link is invalid."
            ));
        LOGGER.info("event=registration.confirmation.record-found utilisateurId={}", confirmation.utilisateurId());

        if (confirmation.confirmedAt() != null) {
            throw AuthException.conflict(
                "REGISTRATION_ALREADY_CONFIRMED",
                "This registration has already been confirmed."
            );
        }

        Instant now = clock.instant();
        if (!now.isBefore(confirmation.expiresAt())) {
            LOGGER.warn("event=registration.confirmation.expired utilisateurId={}", confirmation.utilisateurId());
            deletePendingRegistration(confirmation.utilisateurId());
            throw AuthException.gone(
                "REGISTRATION_CONFIRMATION_EXPIRED",
                "This registration confirmation link has expired. Please register again."
            );
        }

        if (!UtilisateurStatus.PENDING_CONFIRMATION.databaseValue().equals(confirmation.utilisateurStatus())) {
            throw AuthException.conflict(
                "REGISTRATION_CONFIRMATION_UNAVAILABLE",
                "This registration cannot be confirmed."
            );
        }

        authRepository.activateUtilisateur(
            confirmation.utilisateurId(),
            UtilisateurStatus.PENDING_CONFIRMATION.databaseValue(),
            UtilisateurStatus.ACTIVE.databaseValue()
        );
        authRepository.markConfirmationCompleted(confirmation.utilisateurId(), now);
        LOGGER.info("event=registration.confirmation.completed utilisateurId={}", confirmation.utilisateurId());
    }

    private ClientProfile clientProfileFor(RegistrableUserType userType, RegistrationCommand command) {
        if (userType != RegistrableUserType.CLIENT) {
            if (hasAnyClientProfileValue(command)) {
                throw AuthException.badRequest(
                    "REGISTRATION_CLIENT_PROFILE_FORBIDDEN",
                    "Only CLIENT registrations can include a buyer profile."
                );
            }
            return null;
        }

        ClientProfileType profileType = ClientProfileType.from(command.clientProfileType());
        if (profileType == ClientProfileType.PARTICULIER) {
            if (hasEnterpriseDetails(command)) {
                throw AuthException.badRequest(
                    "REGISTRATION_CLIENT_PROFILE_DETAILS_FORBIDDEN",
                    "Enterprise details are only accepted for an enterprise buyer profile."
                );
            }
            return new ClientProfile(profileType, null, null, null);
        }

        return new ClientProfile(
            profileType,
            normalizeEnterpriseValue(command.raisonSociale(), 150),
            normalizeEnterpriseIdentifier(command.niu()),
            normalizeEnterpriseIdentifier(command.rccm())
        );
    }

    private void insertClientProfile(long utilisateurId, ClientProfile profile) {
        if (profile == null) {
            return;
        }
        if (profile.type() == ClientProfileType.PARTICULIER) {
            authRepository.insertClientParticulier(utilisateurId);
            return;
        }
        authRepository.insertClientEntreprise(utilisateurId, profile.raisonSociale(), profile.niu(), profile.rccm());
    }

    private boolean hasAnyClientProfileValue(RegistrationCommand command) {
        return command.clientProfileType() != null
            || command.raisonSociale() != null
            || command.niu() != null
            || command.rccm() != null;
    }

    private boolean hasEnterpriseDetails(RegistrationCommand command) {
        return command.raisonSociale() != null || command.niu() != null || command.rccm() != null;
    }

    private String normalizeEnterpriseValue(String value, int maximumLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw AuthException.badRequest(
                "REGISTRATION_ENTERPRISE_DETAILS_INVALID",
                "The enterprise registration details are not valid."
            );
        }
        return normalized;
    }

    private String normalizeEnterpriseIdentifier(String value) {
        return normalizeEnterpriseValue(value, 50).toUpperCase(Locale.ROOT);
    }

    private void deletePendingRegistration(long utilisateurId) {
        authRepository.deletePasswordHistory(utilisateurId);
        authRepository.deleteConfirmation(utilisateurId);
        authRepository.deletePendingUtilisateur(utilisateurId, UtilisateurStatus.PENDING_CONFIRMATION.databaseValue());
    }

    private String confirmationUrl(String rawToken) {
        return UriComponentsBuilder.fromUriString(registrationProperties.getConfirmationUrl())
            .queryParam("token", rawToken)
            .build()
            .encode()
            .toUriString();
    }

    private String normalizeEmail(String email) {
        return normalizeRequired(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw AuthException.badRequest("REGISTRATION_VALUE_INVALID", "A required registration value is missing.");
        }
        return value.trim();
    }

    private void validatePasswordLength(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_BYTES) {
            throw AuthException.badRequest(
                "REGISTRATION_PASSWORD_INVALID",
                "The password does not meet the supported length requirements."
            );
        }
    }

    private record ClientProfile(
        ClientProfileType type,
        String raisonSociale,
        String niu,
        String rccm
    ) {
    }
}
