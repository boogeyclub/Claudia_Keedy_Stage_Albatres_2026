package cm.odigital.serviceconnectmarket.auth.service;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cm.odigital.serviceconnectmarket.auth.domain.UtilisateurStatus;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;

@Service
public class RegistrationExpiryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationExpiryService.class);

    private final AuthRepository authRepository;
    private final Clock clock;

    public RegistrationExpiryService(AuthRepository authRepository, Clock authenticationClock) {
        this.authRepository = authRepository;
        this.clock = authenticationClock;
    }

    /**
     * Runs in its own transaction so expired pending accounts are removed even
     * when a subsequent registration or login request is denied.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeExpiredRegistrations() {
        List<Long> expiredUtilisateurIds = authRepository.findExpiredPendingUtilisateurIds(
            clock.instant(),
            UtilisateurStatus.PENDING_CONFIRMATION.databaseValue()
        );
        expiredUtilisateurIds.forEach(this::deletePendingRegistration);
        if (!expiredUtilisateurIds.isEmpty()) {
            LOGGER.info("event=registration.cleanup.completed removedCount={}", expiredUtilisateurIds.size());
        }
    }

    private void deletePendingRegistration(long utilisateurId) {
        authRepository.deletePasswordHistory(utilisateurId);
        authRepository.deleteConfirmation(utilisateurId);
        authRepository.deletePendingUtilisateur(utilisateurId, UtilisateurStatus.PENDING_CONFIRMATION.databaseValue());
    }
}
