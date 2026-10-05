package cm.odigital.serviceconnectmarket.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

    private String confirmationUrl = "http://localhost:8080/cacaomarketcm/api/auth/registration/confirm";
    private String mailFrom = "";
    private String cleanupInterval = "PT1M";

    public String getConfirmationUrl() {
        return confirmationUrl;
    }

    public void setConfirmationUrl(String confirmationUrl) {
        this.confirmationUrl = confirmationUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(String cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }
}
