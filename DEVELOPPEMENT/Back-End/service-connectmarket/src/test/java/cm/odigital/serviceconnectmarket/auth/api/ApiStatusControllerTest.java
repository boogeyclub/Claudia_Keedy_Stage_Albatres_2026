package cm.odigital.serviceconnectmarket.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ApiStatusControllerTest {

    @Test
    void reportsThatTheServiceIsAvailable() {
        assertEquals(
            Map.of("status", "UP", "service", "service-connectmarket"),
            new ApiStatusController().health()
        );
    }
}
