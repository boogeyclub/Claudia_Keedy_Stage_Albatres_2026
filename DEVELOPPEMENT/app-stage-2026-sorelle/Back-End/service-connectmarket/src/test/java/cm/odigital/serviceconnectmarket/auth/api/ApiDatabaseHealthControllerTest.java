package cm.odigital.serviceconnectmarket.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class ApiDatabaseHealthControllerTest {

    @Test
    void reportsDatabaseReadinessWhenTheProtectedUserTypeProjectionIsAvailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doNothing().when(jdbcTemplate).execute("SELECT code, tu_name FROM gu.type_utilisateur LIMIT 0");

        var response = new ApiDatabaseHealthController(jdbcTemplate).database();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            Map.of("status", "UP", "service", "service-connectmarket", "database", "UP"),
            response.getBody()
        );
    }

    @Test
    void returnsASafeServiceUnavailableResponseWhenTheSchemaCannotBeRead() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new DataAccessResourceFailureException("database unavailable"))
            .when(jdbcTemplate)
            .execute("SELECT code, tu_name FROM gu.type_utilisateur LIMIT 0");

        var response = new ApiDatabaseHealthController(jdbcTemplate).database();

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
}
