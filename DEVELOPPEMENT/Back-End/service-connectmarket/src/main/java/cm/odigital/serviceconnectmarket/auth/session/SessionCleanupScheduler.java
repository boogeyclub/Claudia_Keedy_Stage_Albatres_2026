package cm.odigital.serviceconnectmarket.auth.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks timed-out browser sessions as invalidated while retaining a safe session history.
 */
@Component
public class SessionCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCleanupScheduler.class);

    private final UserSessionService userSessionService;

    public SessionCleanupScheduler(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Scheduled(fixedDelayString = "${app.session.cleanup-interval:PT5M}")
    public void invalidateExpiredSessions() {
        int invalidatedSessionCount = userSessionService.invalidateExpiredSessions();
        if (invalidatedSessionCount > 0) {
            LOGGER.info("event=session.cleanup.completed invalidatedSessionCount={}", invalidatedSessionCount);
        }
    }
}
