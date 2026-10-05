package cm.odigital.serviceconnectmarket.schema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Verifies that every relation and column the API depends on can actually be read.
 *
 * <p>Each probe is a {@code LIMIT 0} statement: PostgreSQL validates the relation and the column
 * names in its catalog while no application row, hash, or token is ever read. A connectivity probe
 * runs first so an unreachable database is reported as such instead of as a missing table.
 */
@Service
public class SchemaVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaVerificationService.class);

    private static final String CONNECTIVITY_TARGET = "[database-connection]";
    private static final String CONNECTIVITY_PROBE = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;

    public SchemaVerificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SchemaVerificationOutcome verify() {
        if (!canRead(CONNECTIVITY_TARGET, CONNECTIVITY_PROBE)) {
            return SchemaVerificationOutcome.databaseUnavailable();
        }

        List<String> missingElements = new ArrayList<>();

        for (GuSchemaCatalog.GuRelation relation : GuSchemaCatalog.GU_RELATIONS) {
            if (!canRead(relation.qualifiedName(), relation.relationProbe())) {
                // The relation itself is unusable: report it once and skip its columns so that a
                // missing table does not produce one redundant entry per expected column.
                missingElements.add(relation.qualifiedName());
                continue;
            }

            for (String column : relation.columns()) {
                if (!canRead(relation.qualifiedName() + "." + column, relation.columnProbe(column))) {
                    missingElements.add(relation.qualifiedName() + "." + column);
                }
            }
        }

        return missingElements.isEmpty()
            ? SchemaVerificationOutcome.complete()
            : SchemaVerificationOutcome.incomplete(missingElements);
    }

    private boolean canRead(String target, String probe) {
        try {
            jdbcTemplate.execute(probe);
            return true;
        } catch (DataAccessException exception) {
            // The log stays safe: only code-owned identifiers, the exception type, and the SQLSTATE
            // are written. Probe text, row values, and driver messages are never logged.
            LOGGER.warn(
                "event=schema.probe.unavailable target={} exceptionType={} sqlState={}",
                target,
                exception.getClass().getName(),
                sqlStateOf(exception)
            );
            return false;
        }
    }

    private String sqlStateOf(DataAccessException exception) {
        Throwable specificCause = exception.getMostSpecificCause();
        return specificCause instanceof SQLException sqlException && sqlException.getSQLState() != null
            ? sqlException.getSQLState()
            : "[unavailable]";
    }
}
