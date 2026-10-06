package cm.odigital.serviceconnectmarket.schema;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Startup schema-verification policy, configured under {@code app.schema-verification}.
 */
@ConfigurationProperties(prefix = "app.schema-verification")
public class SchemaVerificationProperties {

    /** Whether the service verifies the {@code gu} schema while it starts. */
    private boolean enabled = true;

    /**
     * Whether an incomplete schema prevents the service from starting. Leaving this enabled turns a
     * forgotten or outdated {@code gu.sql} into an immediate, explicit startup failure instead of a
     * late error on the first protected request.
     */
    private boolean failFast = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}
