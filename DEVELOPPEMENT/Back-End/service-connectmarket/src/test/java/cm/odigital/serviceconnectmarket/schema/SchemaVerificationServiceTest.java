package cm.odigital.serviceconnectmarket.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

class SchemaVerificationServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final SchemaVerificationService service = new SchemaVerificationService(jdbcTemplate);

    @Test
    void reportsACompleteSchemaWhenEveryRelationAndColumnIsReadable() {
        acceptEveryProbe();

        SchemaVerificationOutcome outcome = service.verify();

        assertEquals(SchemaVerificationOutcome.Status.COMPLETE, outcome.status());
        assertEquals(List.of(), outcome.missingElements());
        verify(jdbcTemplate).execute("SELECT 1");
        verify(jdbcTemplate).execute("SELECT 1 FROM gu.type_utilisateur LIMIT 0");
        verify(jdbcTemplate).execute("SELECT \"dateCreation\" FROM gu.utilisateurs LIMIT 0");
        verify(jdbcTemplate).execute("SELECT 1 FROM gu.sessions_utilisateur LIMIT 0");
    }

    @Test
    void reportsAMissingTableOnceAndStillChecksTheFollowingRelations() {
        acceptEveryProbe();
        rejectProbe("SELECT 1 FROM gu.client_entreprise LIMIT 0", "42P01");

        SchemaVerificationOutcome outcome = service.verify();

        assertEquals(SchemaVerificationOutcome.Status.SCHEMA_INCOMPLETE, outcome.status());
        assertEquals(List.of("gu.client_entreprise"), outcome.missingElements());
        verify(jdbcTemplate, never()).execute("SELECT niu FROM gu.client_entreprise LIMIT 0");
        verify(jdbcTemplate).execute("SELECT 1 FROM gu.sessions_utilisateur LIMIT 0");
    }

    @Test
    void reportsTheMissingColumnOfAnExistingTable() {
        acceptEveryProbe();
        rejectProbe("SELECT niu FROM gu.client_entreprise LIMIT 0", "42703");

        SchemaVerificationOutcome outcome = service.verify();

        assertEquals(SchemaVerificationOutcome.Status.SCHEMA_INCOMPLETE, outcome.status());
        assertEquals(List.of("gu.client_entreprise.niu"), outcome.missingElements());
        verify(jdbcTemplate).execute("SELECT rccm FROM gu.client_entreprise LIMIT 0");
    }

    @Test
    void reportsTheDatabaseAsUnavailableWithoutCheckingTheSchema() {
        rejectProbe("SELECT 1", null);

        SchemaVerificationOutcome outcome = service.verify();

        assertEquals(SchemaVerificationOutcome.Status.DATABASE_UNAVAILABLE, outcome.status());
        assertEquals(List.of(), outcome.missingElements());
        verify(jdbcTemplate, never()).execute("SELECT 1 FROM gu.type_utilisateur LIMIT 0");
    }

    private void acceptEveryProbe() {
        doNothing().when(jdbcTemplate).execute(anyString());
    }

    private void rejectProbe(String probe, String sqlState) {
        doThrow(new BadSqlGrammarException(
            "schema verification probe",
            probe,
            new SQLException("probe rejected", sqlState)
        )).when(jdbcTemplate).execute(probe);
    }
}
