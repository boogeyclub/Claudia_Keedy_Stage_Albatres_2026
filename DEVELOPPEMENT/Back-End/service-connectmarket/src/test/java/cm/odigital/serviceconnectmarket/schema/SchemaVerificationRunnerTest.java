package cm.odigital.serviceconnectmarket.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class SchemaVerificationRunnerTest {

    private final SchemaVerificationService schemaVerificationService = mock(SchemaVerificationService.class);
    private final SchemaVerificationProperties properties = new SchemaVerificationProperties();

    @Test
    void acceptsACompleteSchema() {
        when(schemaVerificationService.verify()).thenReturn(SchemaVerificationOutcome.complete());

        assertDoesNotThrow(() -> runner().run(null));
    }

    @Test
    void refusesToStartWhenFailFastIsEnabledAndTheSchemaIsIncomplete() {
        when(schemaVerificationService.verify())
            .thenReturn(SchemaVerificationOutcome.incomplete(List.of("gu.client_entreprise")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> runner().run(null));

        assertTrue(exception.getMessage().contains("gu.client_entreprise"));
        assertTrue(exception.getMessage().contains("gu.sql"));
    }

    @Test
    void refusesToStartWhenTheDatabaseCannotBeReached() {
        when(schemaVerificationService.verify()).thenReturn(SchemaVerificationOutcome.databaseUnavailable());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> runner().run(null));

        assertTrue(exception.getMessage().contains("PostgreSQL is unreachable"));
    }

    @Test
    void startsAnywayWhenFailFastIsDisabled() {
        properties.setFailFast(false);
        when(schemaVerificationService.verify())
            .thenReturn(SchemaVerificationOutcome.incomplete(List.of("gu.password_reset")));

        assertDoesNotThrow(() -> runner().run(null));
    }

    @Test
    void skipsTheCheckWhenItIsDisabled() {
        properties.setEnabled(false);

        runner().run(null);

        verifyNoInteractions(schemaVerificationService);
    }

    private SchemaVerificationRunner runner() {
        return new SchemaVerificationRunner(schemaVerificationService, properties);
    }
}
