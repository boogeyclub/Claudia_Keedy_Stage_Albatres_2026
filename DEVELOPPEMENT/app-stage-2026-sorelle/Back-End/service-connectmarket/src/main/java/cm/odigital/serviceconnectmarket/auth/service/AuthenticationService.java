package cm.odigital.serviceconnectmarket.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;
import cm.odigital.serviceconnectmarket.auth.domain.UtilisateurStatus;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.persistence.LoginCandidate;
import cm.odigital.serviceconnectmarket.observability.AuditValue;

@Service
public class AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationExpiryService registrationExpiryService;

    public AuthenticationService(
        AuthRepository authRepository,
        PasswordEncoder passwordEncoder,
        RegistrationExpiryService registrationExpiryService
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationExpiryService = registrationExpiryService;
    }

    public AuthenticatedUtilisateur authenticate(String identity, String password) {
        String normalizedIdentity = identity == null ? "" : identity.trim();
        LOGGER.info("event=login.authentication.started identity={}", AuditValue.maskedIdentity(normalizedIdentity));
        registrationExpiryService.removeExpiredRegistrations();

        LoginCandidate candidate = authRepository.findLoginCandidate(normalizedIdentity)
            .orElseThrow(() -> {
                LOGGER.warn("event=login.authentication.rejected reason=IDENTITY_NOT_FOUND");
                return invalidCredentials();
            });

        if (!passwordEncoder.matches(password == null ? "" : password, candidate.passwordHash())) {
            LOGGER.warn("event=login.authentication.rejected reason=PASSWORD_MISMATCH utilisateurId={}", candidate.id());
            throw invalidCredentials();
        }

        if (UtilisateurStatus.PENDING_CONFIRMATION.databaseValue().equals(candidate.statut())) {
            LOGGER.warn("event=login.authentication.rejected reason=PENDING_CONFIRMATION utilisateurId={}", candidate.id());
            throw AuthException.forbidden(
                "REGISTRATION_PENDING_CONFIRMATION",
                "Confirm the registration email before signing in."
            );
        }

        if (!UtilisateurStatus.ACTIVE.databaseValue().equals(candidate.statut())) {
            LOGGER.warn("event=login.authentication.rejected reason=USER_STATUS_NOT_ACTIVE utilisateurId={}", candidate.id());
            throw invalidCredentials();
        }

        LOGGER.info("event=login.authentication.completed utilisateurId={} role={}", candidate.id(), candidate.userType());
        return new AuthenticatedUtilisateur(
            candidate.id(),
            candidate.email(),
            candidate.login(),
            candidate.prenom(),
            candidate.nom(),
            candidate.userType()
        );
    }

    private AuthException invalidCredentials() {
        return AuthException.unauthorized("INVALID_CREDENTIALS", "The credentials are invalid.");
    }
}
