package cm.odigital.serviceconnectmarket.auth.admin.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.odigital.serviceconnectmarket.auth.admin.domain.AdminTable;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminConfirmationRecord;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminTableRepository;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminUserRecord;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminUserTypeRecord;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;

/**
 * Contains the only supported administration mutations for the gu schema. It intentionally does
 * not offer a generic SQL interface and never accepts or returns password, reset-token, or browser
 * session hashes.
 */
@Service
public class AdminTableService {

    private static final int BCRYPT_MAXIMUM_BYTES = 72;
    private static final Set<String> SYSTEM_ROLES = Set.of("ADMINISTRATEUR", "VENDEUR", "CLIENT");
    private static final Set<String> LOGIN_ROLES = Set.of("ADMINISTRATEUR", "VENDEUR", "CLIENT");
    private static final Set<String> MANAGED_USER_STATUSES = Set.of("ACTIF", "SUSPENDU");
    private static final Set<String> USER_TYPE_FIELDS = Set.of("code", "name");
    private static final Set<String> USER_CREATE_FIELDS = Set.of(
        "typeUtilisateurId", "nom", "prenom", "email", "login", "statut", "password"
    );
    private static final Set<String> USER_UPDATE_FIELDS = Set.of(
        "typeUtilisateurId", "nom", "prenom", "email", "login", "statut"
    );
    private static final Set<String> BASIC_RIGHT_FIELDS = Set.of("code", "name");
    private static final Set<String> RIGHT_ASSIGNMENT_FIELDS = Set.of("typeUtilisateurId", "basicRightId");
    private static final Set<String> ENTERPRISE_PROFILE_FIELDS = Set.of("raisonSociale", "niu", "rccm");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{0,99}$");

    private final AdminTableRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminTableService(
        AdminTableRepository repository,
        PasswordEncoder passwordEncoder,
        Clock authenticationClock
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = authenticationClock;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rowsFor(AdminTable table) {
        return repository.findRows(table);
    }

    @Transactional
    public void create(AdminTable table, Map<String, Object> values, long administratorId) {
        switch (table) {
            case TYPE_UTILISATEUR -> {
                allowOnly(values, USER_TYPE_FIELDS);
                createUserType(values);
            }
            case UTILISATEURS -> {
                allowOnly(values, USER_CREATE_FIELDS);
                createUtilisateur(values, administratorId);
            }
            case BASIC_RIGHTS -> {
                allowOnly(values, BASIC_RIGHT_FIELDS);
                createBasicRight(values);
            }
            case TYPE_UTILISATEUR_BASIC_RIGHT -> {
                allowOnly(values, RIGHT_ASSIGNMENT_FIELDS);
                createTypeUserBasicRight(values);
            }
            case CLIENT_PARTICULIER, CLIENT_ENTREPRISE, SESSIONS_UTILISATEUR, REGISTRATION_CONFIRMATION,
                PASSWORD_RESET, PASSWORD_HISTORY -> throw readOnlyTable(table);
        }
    }

    @Transactional
    public void update(AdminTable table, String recordId, Map<String, Object> values, long administratorId) {
        long id = numericRecordId(recordId);
        switch (table) {
            case TYPE_UTILISATEUR -> {
                allowOnly(values, USER_TYPE_FIELDS);
                updateUserType(id, values);
            }
            case UTILISATEURS -> {
                allowOnly(values, USER_UPDATE_FIELDS);
                updateUtilisateur(id, values, administratorId);
            }
            case CLIENT_ENTREPRISE -> {
                allowOnly(values, ENTERPRISE_PROFILE_FIELDS);
                updateClientEntreprise(id, values);
            }
            case BASIC_RIGHTS -> {
                allowOnly(values, BASIC_RIGHT_FIELDS);
                updateBasicRight(id, values);
            }
            case CLIENT_PARTICULIER, SESSIONS_UTILISATEUR, REGISTRATION_CONFIRMATION, PASSWORD_RESET,
                TYPE_UTILISATEUR_BASIC_RIGHT, PASSWORD_HISTORY -> throw immutableTable(table);
        }
    }

    @Transactional
    public void remove(AdminTable table, String recordId, long administratorId) {
        switch (table) {
            case TYPE_UTILISATEUR -> deleteUserType(numericRecordId(recordId));
            case UTILISATEURS -> deleteUtilisateur(numericRecordId(recordId), administratorId);
            case SESSIONS_UTILISATEUR -> revokeSession(numericRecordId(recordId));
            case REGISTRATION_CONFIRMATION -> cancelPendingRegistration(numericRecordId(recordId));
            case PASSWORD_RESET -> revokePasswordReset(numericRecordId(recordId));
            case TYPE_UTILISATEUR_BASIC_RIGHT -> removeTypeUserBasicRight(recordId);
            case CLIENT_PARTICULIER, CLIENT_ENTREPRISE, BASIC_RIGHTS, PASSWORD_HISTORY -> throw immutableTable(table);
        }
    }

    private void createUserType(Map<String, Object> values) {
        String code = code(values, "code", 50);
        if (SYSTEM_ROLES.contains(code)) {
            throw AuthException.conflict(
                "ADMIN_SYSTEM_ROLE_PROTECTED",
                "The built-in application roles cannot be recreated through the administrator console."
            );
        }
        repository.insertUserType(code, text(values, "name", 100));
    }

    private void updateUserType(long id, Map<String, Object> values) {
        AdminUserTypeRecord existing = requireUserType(id);
        String code = code(values, "code", 50);
        if (SYSTEM_ROLES.contains(existing.code()) && !existing.code().equals(code)) {
            throw AuthException.conflict(
                "ADMIN_SYSTEM_ROLE_PROTECTED",
                "The code of a built-in application role cannot be changed."
            );
        }
        if (!existing.code().equals(code) && SYSTEM_ROLES.contains(code)) {
            throw AuthException.conflict(
                "ADMIN_SYSTEM_ROLE_PROTECTED",
                "A custom role cannot replace a built-in application role."
            );
        }
        requireUpdated(repository.updateUserType(id, code, text(values, "name", 100)), "user type");
    }

    private void deleteUserType(long id) {
        AdminUserTypeRecord existing = requireUserType(id);
        if (SYSTEM_ROLES.contains(existing.code())) {
            throw AuthException.conflict(
                "ADMIN_SYSTEM_ROLE_PROTECTED",
                "Built-in application roles cannot be deleted."
            );
        }
        if (repository.userTypeHasUtilisateurs(id) || repository.userTypeHasBasicRights(id)) {
            throw AuthException.conflict(
                "ADMIN_USER_TYPE_IN_USE",
                "Remove the user type's users and right assignments before deleting it."
            );
        }
        requireUpdated(repository.deleteUserType(id), "user type");
    }

    private void createUtilisateur(Map<String, Object> values, long administratorId) {
        AdminUserTypeRecord type = requireUserType(identifier(values, "typeUtilisateurId"));
        requireLoginCapableType(type);
        if ("CLIENT".equals(type.code())) {
            throw AuthException.conflict(
                "ADMIN_CLIENT_CREATION_REQUIRES_REGISTRATION",
                "Create buyer accounts through the registration workflow so their required legal profile is recorded."
            );
        }
        String nom = text(values, "nom", 100);
        String prenom = text(values, "prenom", 100);
        String email = email(values, "email");
        String login = text(values, "login", 100);
        String status = status(values, "statut");
        String password = password(values, "password");
        ensureIdentityAvailable(email, login, null);

        Instant now = clock.instant();
        long utilisateurId = repository.insertUtilisateur(
            type.id(),
            nom,
            prenom,
            email,
            login,
            status,
            now
        );
        repository.insertPasswordHash(utilisateurId, passwordEncoder.encode(password), now);
    }

    private void updateUtilisateur(long id, Map<String, Object> values, long administratorId) {
        AdminUserRecord existing = requireUtilisateur(id);
        AdminUserTypeRecord type = requireUserType(identifier(values, "typeUtilisateurId"));
        requireLoginCapableType(type);
        ensureClientProfileRoleIsPreserved(existing, type.code());
        String nom = text(values, "nom", 100);
        String prenom = text(values, "prenom", 100);
        String email = email(values, "email");
        String login = text(values, "login", 100);
        String status = status(values, "statut");
        ensureAdministratorContinuity(existing, type.code(), status, administratorId, false);
        ensureIdentityAvailable(email, login, id);

        requireUpdated(
            repository.updateUtilisateur(id, type.id(), nom, prenom, email, login, status),
            "utilisateur"
        );
    }

    private void updateClientEntreprise(long utilisateurId, Map<String, Object> values) {
        String raisonSociale = text(values, "raisonSociale", 150);
        String niu = enterpriseIdentifier(values, "niu");
        String rccm = enterpriseIdentifier(values, "rccm");
        if (repository.enterpriseIdentifiersExist(niu, rccm, utilisateurId)) {
            throw AuthException.conflict(
                "ADMIN_ENTERPRISE_IDENTIFIER_ALREADY_EXISTS",
                "Another enterprise profile already uses one of these registered identifiers."
            );
        }
        requireUpdated(
            repository.updateClientEntreprise(utilisateurId, raisonSociale, niu, rccm),
            "enterprise client profile"
        );
    }

    private void ensureClientProfileRoleIsPreserved(AdminUserRecord existing, String nextTypeCode) {
        if ("CLIENT".equals(existing.typeCode()) != "CLIENT".equals(nextTypeCode)) {
            throw AuthException.conflict(
                "ADMIN_CLIENT_ROLE_CHANGE_REQUIRES_PROFILE_WORKFLOW",
                "Changing a CLIENT account's role requires a deliberate buyer-profile workflow."
            );
        }
    }

    private void deleteUtilisateur(long id, long administratorId) {
        AdminUserRecord existing = requireUtilisateur(id);
        if (id == administratorId) {
            throw AuthException.conflict(
                "ADMIN_SELF_DELETE_FORBIDDEN",
                "An administrator cannot delete their own signed-in account."
            );
        }
        ensureAdministratorContinuity(existing, null, null, administratorId, true);
        repository.deletePasswordHistory(id);
        requireUpdated(repository.deleteUtilisateur(id), "utilisateur");
    }

    private void revokeSession(long id) {
        // The operation is idempotent: an already invalidated browser does not reveal extra data.
        repository.revokeSession(id, clock.instant());
    }

    private void cancelPendingRegistration(long confirmationId) {
        AdminConfirmationRecord confirmation = repository.findRegistrationConfirmation(confirmationId)
            .orElseThrow(() -> notFound("registration confirmation"));
        if (!"EN_ATTENTE_CONFIRMATION".equals(confirmation.utilisateurStatus()) || confirmation.confirmedAt() != null) {
            throw AuthException.conflict(
                "ADMIN_CONFIRMATION_NOT_PENDING",
                "Only an unconfirmed pending registration can be cancelled."
            );
        }
        repository.deletePasswordHistory(confirmation.utilisateurId());
        requireUpdated(repository.deleteUtilisateur(confirmation.utilisateurId()), "pending utilisateur");
    }

    private void revokePasswordReset(long resetId) {
        requireUpdated(repository.deletePasswordReset(resetId), "password reset request");
    }

    private void createBasicRight(Map<String, Object> values) {
        String code = code(values, "code", 100);
        if ("APP-CONN".equals(code)) {
            throw AuthException.conflict(
                "ADMIN_BASIC_RIGHT_PROTECTED",
                "The APP-CONN basic right is managed by the application bootstrap schema."
            );
        }
        repository.insertBasicRight(code, text(values, "name", 150));
    }

    private void updateBasicRight(long id, Map<String, Object> values) {
        String currentCode = repository.findBasicRightCode(id).orElseThrow(() -> notFound("basic right"));
        String code = code(values, "code", 100);
        if ("APP-CONN".equals(currentCode) && !currentCode.equals(code)) {
            throw AuthException.conflict(
                "ADMIN_BASIC_RIGHT_PROTECTED",
                "The APP-CONN basic right code cannot be changed."
            );
        }
        if (!"APP-CONN".equals(currentCode) && "APP-CONN".equals(code)) {
            throw AuthException.conflict(
                "ADMIN_BASIC_RIGHT_PROTECTED",
                "A custom basic right cannot replace the protected APP-CONN capability."
            );
        }
        requireUpdated(repository.updateBasicRight(id, code, text(values, "name", 150)), "basic right");
    }

    private void createTypeUserBasicRight(Map<String, Object> values) {
        long typeUtilisateurId = identifier(values, "typeUtilisateurId");
        long basicRightId = identifier(values, "basicRightId");
        requireUserType(typeUtilisateurId);
        if (!repository.basicRightExists(basicRightId)) {
            throw notFound("basic right");
        }
        if (!repository.insertTypeUserBasicRight(typeUtilisateurId, basicRightId, clock.instant())) {
            throw AuthException.conflict(
                "ADMIN_RIGHT_ASSIGNMENT_ALREADY_EXISTS",
                "This basic right is already assigned to the selected user type."
            );
        }
    }

    private void removeTypeUserBasicRight(String recordId) {
        long[] identifiers = compositeRecordId(recordId);
        AdminUserTypeRecord type = requireUserType(identifiers[0]);
        String basicRightCode = repository.findBasicRightCode(identifiers[1]).orElseThrow(() -> notFound("basic right"));
        if ("ADMINISTRATEUR".equals(type.code())) {
            throw AuthException.conflict(
                "ADMIN_RIGHT_ASSIGNMENT_PROTECTED",
                "Basic rights cannot be removed from the administrator type."
            );
        }
        if (SYSTEM_ROLES.contains(type.code()) && "APP-CONN".equals(basicRightCode)) {
            throw AuthException.conflict(
                "ADMIN_RIGHT_ASSIGNMENT_PROTECTED",
                "The APP-CONN basic right is required for built-in application user types."
            );
        }
        requireUpdated(
            repository.deleteTypeUserBasicRight(identifiers[0], identifiers[1]),
            "user type basic-right assignment"
        );
    }

    private void ensureAdministratorContinuity(
        AdminUserRecord existing,
        String nextTypeCode,
        String nextStatus,
        long administratorId,
        boolean deleting
    ) {
        boolean currentlyActiveAdministrator = "ADMINISTRATEUR".equals(existing.typeCode())
            && "ACTIF".equals(existing.statut());
        boolean remainsActiveAdministrator = !deleting
            && "ADMINISTRATEUR".equals(nextTypeCode)
            && "ACTIF".equals(nextStatus);

        if (existing.id() == administratorId && !remainsActiveAdministrator) {
            throw AuthException.conflict(
                "ADMIN_SELF_PRIVILEGE_CHANGE_FORBIDDEN",
                "The signed-in administrator cannot remove or suspend their own administrator access."
            );
        }
        if (currentlyActiveAdministrator && !remainsActiveAdministrator) {
            repository.lockActiveAdministrators();
            if (repository.countActiveAdministrators() <= 1) {
                throw AuthException.conflict(
                    "ADMIN_LAST_ADMINISTRATOR_PROTECTED",
                    "At least one active administrator account must remain available."
                );
            }
        }
    }

    private void requireLoginCapableType(AdminUserTypeRecord type) {
        if (!LOGIN_ROLES.contains(type.code()) || !repository.utilisateurHasAppConnection(type.id())) {
            throw AuthException.conflict(
                "ADMIN_USER_TYPE_NOT_LOGIN_CAPABLE",
                "Selected user type cannot sign in to the application."
            );
        }
    }

    private void ensureIdentityAvailable(String email, String login, Long excludingUtilisateurId) {
        if (repository.identityExists(email, login, excludingUtilisateurId)) {
            throw AuthException.conflict(
                "ADMIN_IDENTITY_ALREADY_EXISTS",
                "An account already uses this email address or login."
            );
        }
    }

    private AdminUserTypeRecord requireUserType(long id) {
        return repository.findUserType(id).orElseThrow(() -> notFound("user type"));
    }

    private AdminUserRecord requireUtilisateur(long id) {
        return repository.findUtilisateur(id).orElseThrow(() -> notFound("utilisateur"));
    }

    private void allowOnly(Map<String, Object> values, Set<String> allowedFields) {
        if (values.keySet().stream().anyMatch(field -> !allowedFields.contains(field))) {
            throw AuthException.badRequest(
                "ADMIN_MUTATION_FIELD_FORBIDDEN",
                "The request contains an administrative field that is not permitted for this operation."
            );
        }
    }

    private String text(Map<String, Object> values, String field, int maximumLength) {
        Object value = values.get(field);
        if (!(value instanceof String rawValue)) {
            throw invalid(field);
        }
        String normalized = rawValue.trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw invalid(field);
        }
        return normalized;
    }

    private String code(Map<String, Object> values, String field, int maximumLength) {
        String code = text(values, field, maximumLength).toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw invalid(field);
        }
        return code;
    }

    private String email(Map<String, Object> values, String field) {
        String email = text(values, field, 255).toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw invalid(field);
        }
        return email;
    }

    private String status(Map<String, Object> values, String field) {
        String status = text(values, field, 30).toUpperCase(Locale.ROOT);
        if (!MANAGED_USER_STATUSES.contains(status)) {
            throw invalid(field);
        }
        return status;
    }

    private String enterpriseIdentifier(Map<String, Object> values, String field) {
        return text(values, field, 50).toUpperCase(Locale.ROOT);
    }

    private String password(Map<String, Object> values, String field) {
        // Password bytes are significant. Unlike display fields, do not trim or normalize them.
        Object value = values.get(field);
        if (!(value instanceof String password)
            || password.isBlank()
            || password.length() < 8
            || password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_BYTES) {
            throw invalid(field);
        }
        return password;
    }

    private long identifier(Map<String, Object> values, String field) {
        return numeric(values.get(field), field);
    }

    private long numericRecordId(String recordId) {
        return numeric(recordId, "recordId");
    }

    private long[] compositeRecordId(String recordId) {
        String[] parts = recordId == null ? new String[0] : recordId.split(":", -1);
        if (parts.length != 2) {
            throw invalid("recordId");
        }
        return new long[] { numeric(parts[0], "recordId"), numeric(parts[1], "recordId") };
    }

    private long numeric(Object rawValue, String field) {
        String candidate = rawValue instanceof String stringValue ? stringValue.trim()
            : rawValue instanceof Number number ? number.toString()
            : null;
        if (candidate != null) {
            try {
                // Parsing the exact textual representation rejects fractional JSON numbers rather
                // than silently truncating them to a record identifier.
                long value = Long.parseLong(candidate);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // A safe validation error is returned below.
            }
        }
        throw invalid(field);
    }

    private void requireUpdated(boolean updated, String resource) {
        if (!updated) {
            throw notFound(resource);
        }
    }

    private AuthException invalid(String field) {
        return AuthException.badRequest("ADMIN_MUTATION_INVALID", "The administrative field '" + field + "' is not valid.");
    }

    private AuthException notFound(String resource) {
        return AuthException.badRequest("ADMIN_RECORD_NOT_FOUND", "The requested " + resource + " was not found.");
    }

    private AuthException readOnlyTable(AdminTable table) {
        return AuthException.badRequest(
            "ADMIN_TABLE_READ_ONLY",
            "New records cannot be created directly for " + table.apiKey() + "."
        );
    }

    private AuthException immutableTable(AdminTable table) {
        return AuthException.badRequest(
            "ADMIN_TABLE_IMMUTABLE",
            "Records in " + table.apiKey() + " are managed through controlled application workflows."
        );
    }
}
