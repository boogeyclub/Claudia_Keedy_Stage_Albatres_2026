package cm.odigital.serviceconnectmarket.auth.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SessionIdHasherTest {

    @Test
    void convertsTheRawServletIdentifierToAStableSha256Value() {
        String rawSessionId = "browser-only-session-id";

        String hash = new SessionIdHasher().hash(rawSessionId);

        assertNotEquals(rawSessionId, hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(hash, new SessionIdHasher().hash(rawSessionId));
    }
}
