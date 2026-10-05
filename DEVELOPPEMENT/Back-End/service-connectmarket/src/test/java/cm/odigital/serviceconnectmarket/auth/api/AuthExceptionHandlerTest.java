package cm.odigital.serviceconnectmarket.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

class AuthExceptionHandlerTest {

    @Test
    void mapsDatabaseFailuresToASafeServiceUnavailableResponse() {
        Instant now = Instant.parse("2026-09-29T12:00:00Z");
        AuthExceptionHandler handler = new AuthExceptionHandler(Clock.fixed(now, ZoneOffset.UTC));
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException(
            "sensitive database driver message",
            new SQLException("sensitive SQL detail", "42P01")
        );

        var response = handler.handleDataAccessException(failure);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("DATA_ACCESS_UNAVAILABLE", response.getBody().code());
        assertEquals("The protected data service is temporarily unavailable.", response.getBody().message());
        assertEquals(now, response.getBody().timestamp());
    }
}
