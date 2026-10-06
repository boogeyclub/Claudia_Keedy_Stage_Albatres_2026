package cm.odigital.serviceconnectmarket.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import cm.odigital.serviceconnectmarket.schema.SchemaVerificationOutcome;
import cm.odigital.serviceconnectmarket.schema.SchemaVerificationService;

class ApiDatabaseHealthControllerTest {

    private final SchemaVerificationService schemaVerificationService = mock(SchemaVerificationService.class);

    @Test
    void reportsDatabaseReadinessWhenEverySchemaRelationIsReadable() {
        when(schemaVerificationService.verify()).thenReturn(SchemaVerificationOutcome.complete());

        var response = controller().database();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            Map.of("status", "UP", "service", "service-connectmarket", "database", "UP"),
            response.getBody()
        );
    }

    @Test
    void returnsASafeServiceUnavailableResponseWhenTheDatabaseCannotBeRead() {
        when(schemaVerificationService.verify()).thenReturn(SchemaVerificationOutcome.databaseUnavailable());

        var response = controller().database();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(
            Map.of(
                "status", "DEGRADED",
                "service", "service-connectmarket",
                "database", "UNAVAILABLE",
                "code", "DATA_ACCESS_UNAVAILABLE"
            ),
            response.getBody()
        );
    }

    @Test
    void reportsTheMissingSchemaElementsWhenTheTrackedScriptWasNotApplied() {
        when(schemaVerificationService.verify())
            .thenReturn(SchemaVerificationOutcome.incomplete(List.of("gu.client_entreprise", "gu.utilisateurs.statut")));

        var response = controller().database();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(
            Map.of(
                "status", "DEGRADED",
                "service", "service-connectmarket",
                "database", "UP",
                "code", "SCHEMA_TABLES_MISSING",
                "missingElements", "gu.client_entreprise, gu.utilisateurs.statut"
            ),
            response.getBody()
        );
    }

    private ApiDatabaseHealthController controller() {
        return new ApiDatabaseHealthController(schemaVerificationService);
    }
}
