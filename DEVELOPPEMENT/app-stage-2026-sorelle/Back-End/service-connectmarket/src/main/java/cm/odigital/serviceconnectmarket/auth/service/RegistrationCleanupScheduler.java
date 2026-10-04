package cm.odigital.serviceconnectmarket.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RegistrationCleanupScheduler {

    private final RegistrationExpiryService registrationExpiryService;

    public RegistrationCleanupScheduler(RegistrationExpiryService registrationExpiryService) {
        this.registrationExpiryService = registrationExpiryService;
    }

    @Scheduled(fixedDelayString = "${app.registration.cleanup-interval:PT1M}")
    public void removeExpiredRegistrations() {
        registrationExpiryService.removeExpiredRegistrations();
    }
}
