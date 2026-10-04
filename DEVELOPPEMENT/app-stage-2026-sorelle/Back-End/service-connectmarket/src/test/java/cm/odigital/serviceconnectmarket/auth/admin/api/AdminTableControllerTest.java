package cm.odigital.serviceconnectmarket.auth.admin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cm.odigital.serviceconnectmarket.auth.admin.api.dto.AdminTableRowsResponse;
import cm.odigital.serviceconnectmarket.auth.admin.domain.AdminTable;
import cm.odigital.serviceconnectmarket.auth.admin.service.AdminTableService;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;
import cm.odigital.serviceconnectmarket.auth.session.AuthenticatedSession;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionService;

@ExtendWith(MockitoExtension.class)
class AdminTableControllerTest {

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private AdminTableService adminTableService;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpSession servletSession;

    private AdminTableController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminTableController(userSessionService, adminTableService);
        when(servletRequest.getSession(false)).thenReturn(servletSession);
    }

    @Test
    void deniesTableAccessToAnAuthenticatedNonAdministrator() {
        when(userSessionService.requireAuthenticatedSession(servletSession)).thenReturn(sessionFor("CLIENT"));

        AuthException exception = assertThrows(
            AuthException.class,
            () -> controller.rows("password_history", servletRequest)
        );

        assertEquals("ADMINISTRATOR_ACCESS_REQUIRED", exception.getCode());
    }

    @Test
    void letsAnAdministratorReadOnlyTheServiceProjection() {
        when(userSessionService.requireAuthenticatedSession(servletSession)).thenReturn(sessionFor("ADMINISTRATEUR"));
        List<Map<String, Object>> safeRows = List.of(Map.<String, Object>of(
            "id", 8L,
            "utilisateurLogin", "amina-cocoa",
            "current", true
        ));
        when(adminTableService.rowsFor(AdminTable.PASSWORD_HISTORY)).thenReturn(safeRows);

        AdminTableRowsResponse response = controller.rows("password_history", servletRequest);

        assertEquals("password_history", response.table());
        assertEquals(safeRows, response.records());
        verify(adminTableService).rowsFor(AdminTable.PASSWORD_HISTORY);
    }

    private AuthenticatedSession sessionFor(String role) {
        return new AuthenticatedSession(
            22L,
            new AuthenticatedUtilisateur(7L, "admin@example.com", "administrator", "Ada", "Admin", role)
        );
    }
}
