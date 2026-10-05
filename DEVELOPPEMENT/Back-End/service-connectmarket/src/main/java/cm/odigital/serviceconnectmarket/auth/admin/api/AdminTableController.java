package cm.odigital.serviceconnectmarket.auth.admin.api;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cm.odigital.serviceconnectmarket.auth.admin.api.dto.AdminMutationResponse;
import cm.odigital.serviceconnectmarket.auth.admin.api.dto.AdminRecordMutationRequest;
import cm.odigital.serviceconnectmarket.auth.admin.api.dto.AdminTableRowsResponse;
import cm.odigital.serviceconnectmarket.auth.admin.domain.AdminTable;
import cm.odigital.serviceconnectmarket.auth.admin.service.AdminTableService;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.session.AuthenticatedSession;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionService;

/**
 * Protected, explicitly whitelisted management endpoints for schema gu. Authentication is checked
 * again on every operation; client-side dashboard access alone grants no database access.
 */
@RestController
@RequestMapping("/api/admin/tables")
public class AdminTableController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminTableController.class);
    private static final String ADMINISTRATOR_ROLE = "ADMINISTRATEUR";

    private final UserSessionService userSessionService;
    private final AdminTableService adminTableService;

    public AdminTableController(UserSessionService userSessionService, AdminTableService adminTableService) {
        this.userSessionService = userSessionService;
        this.adminTableService = adminTableService;
    }

    @GetMapping("/{tableKey}")
    public AdminTableRowsResponse rows(
        @PathVariable String tableKey,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession administrator = requireAdministrator(servletRequest);
        AdminTable table = AdminTable.fromApiKey(tableKey);
        List<Map<String, Object>> records = adminTableService.rowsFor(table);
        LOGGER.info(
            "event=admin-table.list.completed administratorId={} table={} recordCount={}",
            administrator.utilisateur().id(),
            table.apiKey(),
            records.size()
        );
        return new AdminTableRowsResponse(table.apiKey(), records);
    }

    @PostMapping("/{tableKey}")
    public ResponseEntity<AdminMutationResponse> create(
        @PathVariable String tableKey,
        @RequestBody AdminRecordMutationRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession administrator = requireAdministrator(servletRequest);
        AdminTable table = AdminTable.fromApiKey(tableKey);
        adminTableService.create(table, request.values(), administrator.utilisateur().id());
        LOGGER.info(
            "event=admin-table.create.completed administratorId={} table={}",
            administrator.utilisateur().id(),
            table.apiKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(success("created"));
    }

    @PutMapping("/{tableKey}/{recordId}")
    public AdminMutationResponse update(
        @PathVariable String tableKey,
        @PathVariable String recordId,
        @RequestBody AdminRecordMutationRequest request,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession administrator = requireAdministrator(servletRequest);
        AdminTable table = AdminTable.fromApiKey(tableKey);
        adminTableService.update(table, recordId, request.values(), administrator.utilisateur().id());
        LOGGER.info(
            "event=admin-table.update.completed administratorId={} table={}",
            administrator.utilisateur().id(),
            table.apiKey()
        );
        return success("updated");
    }

    /**
     * DELETE has a table-specific meaning: it is a revocation for browser sessions/reset links,
     * cancellation for pending registrations, an unlink for right mappings, and true removal only
     * for the safe configuration/user records permitted by AdminTableService.
     */
    @DeleteMapping("/{tableKey}/{recordId}")
    public ResponseEntity<Void> remove(
        @PathVariable String tableKey,
        @PathVariable String recordId,
        HttpServletRequest servletRequest
    ) {
        AuthenticatedSession administrator = requireAdministrator(servletRequest);
        AdminTable table = AdminTable.fromApiKey(tableKey);
        adminTableService.remove(table, recordId, administrator.utilisateur().id());
        LOGGER.info(
            "event=admin-table.remove.completed administratorId={} table={}",
            administrator.utilisateur().id(),
            table.apiKey()
        );
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedSession requireAdministrator(HttpServletRequest servletRequest) {
        AuthenticatedSession session = userSessionService.requireAuthenticatedSession(servletRequest.getSession(false));
        if (!ADMINISTRATOR_ROLE.equals(session.utilisateur().role())) {
            LOGGER.warn(
                "event=admin-table.access.denied utilisateurId={} role={}",
                session.utilisateur().id(),
                session.utilisateur().role()
            );
            throw AuthException.forbidden(
                "ADMINISTRATOR_ACCESS_REQUIRED",
                "An active administrator session is required for this operation."
            );
        }
        return session;
    }

    private AdminMutationResponse success(String status) {
        return new AdminMutationResponse(status, "Administrative table operation completed.");
    }
}
