package cm.odigital.serviceconnectmarket.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    private String resetUrl = "http://localhost:4200/CacaoMarketCM/password-reset/confirm";
    private Duration tokenTtl = Duration.ofHours(1);

    public String getResetUrl() {
        return resetUrl;
    }

    public void setResetUrl(String resetUrl) {
        this.resetUrl = resetUrl;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }
}
