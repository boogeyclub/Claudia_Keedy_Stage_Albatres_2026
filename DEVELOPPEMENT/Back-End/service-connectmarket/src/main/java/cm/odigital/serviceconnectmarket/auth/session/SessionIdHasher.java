package cm.odigital.serviceconnectmarket.auth.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Converts a servlet session identifier into a one-way database value.
 *
 * <p>The browser's raw JSESSIONID is deliberately never persisted or written to logs.</p>
 */
@Component
public class SessionIdHasher {

    public String hash(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("A servlet session identifier is required.");
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the Java runtime.", exception);
        }
    }
}
