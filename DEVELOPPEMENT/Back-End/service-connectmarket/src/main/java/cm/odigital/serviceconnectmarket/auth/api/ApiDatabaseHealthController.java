package cm.odigital.serviceconnectmarket.auth.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cm.odigital.serviceconnectmarket.schema.SchemaVerificationOutcome;
import cm.odigital.serviceconnectmarket.schema.SchemaVerificationService;

/**
 * Readiness probe for the database dependency used by protected API endpoints. It reports whether
 * every relation created by the tracked {@code gu.sql} script is readable, without ever returning
 * row data: each check is a {@code LIMIT 0} statement.
 */
@RestController
@RequestMapping("/api/health")
public class ApiDatabaseHealthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiDatabaseHealthController.class);

    private final SchemaVerificationService schemaVerificationService;

    public ApiDatabaseHealthController(SchemaVerificationService schemaVerificationService) {
        this.schemaVerificationService = schemaVerificationService;
    }

    @GetMapping("/database")
    public ResponseEntity<Map<String, String>> database() {
        SchemaVerificationOutcome outcome = schemaVerificationService.verify();

        if (outcome.verified()) {
            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "service-connectmarket",
                "database", "UP"
            ));
        }

        if (outcome.status() == SchemaVerificationOutcome.Status.DATABASE_UNAVAILABLE) {
            // The response and the log stay safe: no SQL text, values, or driver message.
            LOGGER.warn("event=api.health.database-unavailable errorCode=DATA_ACCESS_UNAVAILABLE");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "DEGRADED",
                "service", "service-connectmarket",
                "database", "UNAVAILABLE",
                "code", "DATA_ACCESS_UNAVAILABLE"
            ));
        }

        LOGGER.warn(
            "event=api.health.schema-incomplete errorCode=SCHEMA_TABLES_MISSING missingElements=\"{}\"",
            outcome.missingElementsSummary()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "status", "DEGRADED",
            "service", "service-connectmarket",
            "database", "UP",
            "code", "SCHEMA_TABLES_MISSING",
            "missingElements", outcome.missingElementsSummary()
        ));
    }
}
