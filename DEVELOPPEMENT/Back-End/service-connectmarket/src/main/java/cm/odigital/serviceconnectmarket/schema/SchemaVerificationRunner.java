package cm.odigital.serviceconnectmarket.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the schema verification once the application context is ready, so a database that never
 * received {@code DEVELOPPEMENT/Back-End/database/gu.sql} is reported when the service starts instead of on
 * the first protected request.
 */
@Component
public class SchemaVerificationRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaVerificationRunner.class);

    private final SchemaVerificationService schemaVerificationService;
    private final SchemaVerificationProperties properties;

    public SchemaVerificationRunner(
        SchemaVerificationService schemaVerificationService,
        SchemaVerificationProperties properties
    ) {
        this.schemaVerificationService = schemaVerificationService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            LOGGER.info("event=schema.verification.skipped reason=enabled-false");
            return;
        }

        SchemaVerificationOutcome outcome = schemaVerificationService.verify();

        if (outcome.verified()) {
            LOGGER.info(
                "event=schema.verification.completed outcome=complete schema={} relationsChecked={}",
                GuSchemaCatalog.GU_SCHEMA,
                GuSchemaCatalog.GU_RELATIONS.size()
            );
            return;
        }

        if (outcome.status() == SchemaVerificationOutcome.Status.DATABASE_UNAVAILABLE) {
            LOGGER.error(
                "event=schema.verification.failed outcome=database-unavailable "
                    + "hint=\"Check that PostgreSQL is running and that POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, "
                    + "POSTGRES_USER, and POSTGRES_PASSWORD match the local .env file.\""
            );
        } else {
            LOGGER.error(
                "event=schema.verification.failed outcome=schema-incomplete missingElements=\"{}\" "
                    + "hint=\"Apply {} with psql before starting the API; see the database README.\"",
                outcome.missingElementsSummary(),
                GuSchemaCatalog.SCRIPT_RELATIVE_PATH
            );
        }

        if (properties.isFailFast()) {
            throw new IllegalStateException(diagnosticMessage(outcome));
        }

        LOGGER.warn("event=schema.verification.policy failFast=false outcome=degraded serviceContinues=true");
    }

    private String diagnosticMessage(SchemaVerificationOutcome outcome) {
        if (outcome.status() == SchemaVerificationOutcome.Status.DATABASE_UNAVAILABLE) {
            return "Startup schema verification failed: PostgreSQL is unreachable. Check that the database server "
                + "is running on POSTGRES_HOST/POSTGRES_PORT and that POSTGRES_DB, POSTGRES_USER, and "
                + "POSTGRES_PASSWORD match the local .env file.";
        }

        return "Startup schema verification failed: the PostgreSQL schema "
            + GuSchemaCatalog.GU_SCHEMA
            + " is incomplete (missing or unreadable: "
            + outcome.missingElementsSummary()
            + "). Apply "
            + GuSchemaCatalog.SCRIPT_RELATIVE_PATH
            + " with psql before starting the service, or set app.schema-verification.fail-fast=false to start "
            + "with an incomplete schema.";
    }
}
