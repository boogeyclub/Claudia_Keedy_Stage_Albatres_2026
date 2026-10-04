package cm.odigital.serviceconnectmarket.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuditValueTest {

    @Test
    void masksEmailLocalPartsAndPreservesOnlyTheDomain() {
        assertEquals("d***@example.com", AuditValue.maskedEmail("daniel@example.com"));
        assertEquals("*@example.com", AuditValue.maskedEmail("d@example.com"));
        assertEquals("[invalid-email]", AuditValue.maskedEmail("not-an-email"));
    }

    @Test
    void masksLoginValues() {
        assertEquals("ro***", AuditValue.maskedIdentity("root"));
        assertEquals("**", AuditValue.maskedIdentity("id"));
        assertEquals("[missing]", AuditValue.maskedIdentity("  "));
    }
}
