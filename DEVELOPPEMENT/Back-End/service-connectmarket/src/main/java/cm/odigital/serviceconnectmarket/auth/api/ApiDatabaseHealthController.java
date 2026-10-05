package cm.odigital.serviceconnectmarket.auth.api;

import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Readiness probe for the database dependency used by protected API endpoints. It deliberately
 * validates the columns read by the administrator user-type table without returning any row data.
 */
@RestController
@RequestMapping("/api/health")
public class ApiDatabaseHealthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiDatabaseHealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public ApiDatabaseHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/database")
    public ResponseEntity<Map<String, String>> database() {
        try {
            // LIMIT 0 validates the relation and its columns while returning no protected data.
            jdbcTemplate.execute("SELECT code, tu_name FROM gu.type_utilisateur LIMIT 0");
            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "service-connectmarket",
                "database", "UP"
            ));
        } catch (DataAccessException exception) {
            // The API error response and log stay safe: no SQL text, values, or driver message.
            LOGGER.warn(
                "event=api.health.database-unavailable errorCode=DATA_ACCESS_UNAVAILABLE exceptionType={} sqlState={}",
                exception.getClass().getName(),
                sqlStateFor(exception)
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "DEGRADED",
                "service", "service-connectmarket",
                "database", "UNAVAILABLE",
                "code", "DATA_ACCESS_UNAVAILABLE"
            ));
        }
    }

    private String sqlStateFor(DataAccessException exception) {
        Throwable specificCause = exception.getMostSpecificCause();
        return specificCause instanceof SQLException sqlException && sqlException.getSQLState() != null
            ? sqlException.getSQLState()
            : "[unavailable]";
    }
}
