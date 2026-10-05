package cm.odigital.serviceconnectmarket.auth.admin.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import cm.odigital.serviceconnectmarket.auth.admin.domain.AdminTable;

/**
 * Explicit, safe projections and mutations for the gu administrator console. This repository
 * deliberately uses a fixed query per table rather than constructing SQL from request values.
 */
@Repository
public class AdminTableRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminTableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findRows(AdminTable table) {
        return switch (table) {
            case TYPE_UTILISATEUR -> findUserTypes();
            case UTILISATEURS -> findUtilisateurs();
            case CLIENT_PARTICULIER -> findClientParticuliers();
            case CLIENT_ENTREPRISE -> findClientEntreprises();
            case SESSIONS_UTILISATEUR -> findSessions();
            case REGISTRATION_CONFIRMATION -> findRegistrationConfirmations();
            case PASSWORD_RESET -> findPasswordResets();
            case BASIC_RIGHTS -> findBasicRights();
            case TYPE_UTILISATEUR_BASIC_RIGHT -> findTypeUserBasicRights();
            case PASSWORD_HISTORY -> findPasswordHistory();
        };
    }

    public List<Map<String, Object>> findUserTypes() {
        return jdbcTemplate.query(
            """
                SELECT id, code, tu_name
                FROM gu.type_utilisateur
                ORDER BY code ASC, id ASC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "code", resultSet.getString("code"),
                "name", resultSet.getString("tu_name")
            )
        );
    }

    public List<Map<String, Object>> findUtilisateurs() {
        return jdbcTemplate.query(
            """
                SELECT
                    u.id,
                    u.type_utilisateur_id,
                    tu.code AS type_code,
                    u.nom,
                    u.prenom,
                    u.email,
                    u.login,
                    u.statut,
                    u."dateCreation" AS date_creation
                FROM gu.utilisateurs u
                INNER JOIN gu.type_utilisateur tu ON tu.id = u.type_utilisateur_id
                ORDER BY u."dateCreation" DESC, u.id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "typeUtilisateurId", resultSet.getLong("type_utilisateur_id"),
                "typeCode", resultSet.getString("type_code"),
                "nom", resultSet.getString("nom"),
                "prenom", resultSet.getString("prenom"),
                "email", resultSet.getString("email"),
                "login", resultSet.getString("login"),
                "statut", resultSet.getString("statut"),
                "dateCreation", instant(resultSet, "date_creation")
            )
        );
    }

    public List<Map<String, Object>> findClientParticuliers() {
        return jdbcTemplate.query(
            """
                SELECT
                    cp.utilisateur_id AS id,
                    u.login,
                    u.email,
                    u.prenom,
                    u.nom,
                    u.statut,
                    u."dateCreation" AS date_creation
                FROM gu.client_particulier cp
                INNER JOIN gu.utilisateurs u ON u.id = cp.utilisateur_id
                ORDER BY u."dateCreation" DESC, cp.utilisateur_id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "login", resultSet.getString("login"),
                "email", resultSet.getString("email"),
                "prenom", resultSet.getString("prenom"),
                "nom", resultSet.getString("nom"),
                "statut", resultSet.getString("statut"),
                "dateCreation", instant(resultSet, "date_creation")
            )
        );
    }

    public List<Map<String, Object>> findClientEntreprises() {
        return jdbcTemplate.query(
            """
                SELECT
                    ce.utilisateur_id AS id,
                    u.login,
                    u.email,
                    u.prenom,
                    u.nom,
                    u.statut,
                    ce.raison_sociale,
                    ce.niu,
                    ce.rccm,
                    u."dateCreation" AS date_creation
                FROM gu.client_entreprise ce
                INNER JOIN gu.utilisateurs u ON u.id = ce.utilisateur_id
                ORDER BY u."dateCreation" DESC, ce.utilisateur_id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "login", resultSet.getString("login"),
                "email", resultSet.getString("email"),
                "prenom", resultSet.getString("prenom"),
                "nom", resultSet.getString("nom"),
                "statut", resultSet.getString("statut"),
                "raisonSociale", resultSet.getString("raison_sociale"),
                "niu", resultSet.getString("niu"),
                "rccm", resultSet.getString("rccm"),
                "dateCreation", instant(resultSet, "date_creation")
            )
        );
    }

    public List<Map<String, Object>> findSessions() {
        return jdbcTemplate.query(
            """
                SELECT
                    s.id,
                    s.utilisateur_id,
                    u.login AS utilisateur_login,
                    u.email AS utilisateur_email,
                    s.browser_label,
                    s.remember_me,
                    s.date_creation,
                    s.last_seen_at,
                    s.expires_at,
                    s.invalidated_at
                FROM gu.sessions_utilisateur s
                INNER JOIN gu.utilisateurs u ON u.id = s.utilisateur_id
                ORDER BY s.last_seen_at DESC, s.id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "utilisateurId", resultSet.getLong("utilisateur_id"),
                "utilisateurLogin", resultSet.getString("utilisateur_login"),
                "utilisateurEmail", resultSet.getString("utilisateur_email"),
                "browserLabel", resultSet.getString("browser_label"),
                "rememberMe", resultSet.getBoolean("remember_me"),
                "dateCreation", instant(resultSet, "date_creation"),
                "lastSeenAt", instant(resultSet, "last_seen_at"),
                "expiresAt", instant(resultSet, "expires_at"),
                "invalidatedAt", instant(resultSet, "invalidated_at")
            )
        );
    }

    public List<Map<String, Object>> findRegistrationConfirmations() {
        return jdbcTemplate.query(
            """
                SELECT
                    rc.id,
                    rc.utilisateur_id,
                    u.login AS utilisateur_login,
                    u.email AS utilisateur_email,
                    u.statut AS utilisateur_status,
                    rc.expires_at,
                    rc.confirmed_at,
                    rc.date_creation
                FROM gu.registration_confirmation rc
                INNER JOIN gu.utilisateurs u ON u.id = rc.utilisateur_id
                ORDER BY rc.date_creation DESC, rc.id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "utilisateurId", resultSet.getLong("utilisateur_id"),
                "utilisateurLogin", resultSet.getString("utilisateur_login"),
                "utilisateurEmail", resultSet.getString("utilisateur_email"),
                "utilisateurStatus", resultSet.getString("utilisateur_status"),
                "expiresAt", instant(resultSet, "expires_at"),
                "confirmedAt", instant(resultSet, "confirmed_at"),
                "dateCreation", instant(resultSet, "date_creation")
            )
        );
    }

    public List<Map<String, Object>> findPasswordResets() {
        return jdbcTemplate.query(
            """
                SELECT
                    pr.id,
                    pr.utilisateur_id,
                    u.login AS utilisateur_login,
                    u.email AS utilisateur_email,
                    pr.expires_at,
                    pr.used_at,
                    pr.date_creation
                FROM gu.password_reset pr
                INNER JOIN gu.utilisateurs u ON u.id = pr.utilisateur_id
                ORDER BY pr.date_creation DESC, pr.id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "utilisateurId", resultSet.getLong("utilisateur_id"),
                "utilisateurLogin", resultSet.getString("utilisateur_login"),
                "utilisateurEmail", resultSet.getString("utilisateur_email"),
                "expiresAt", instant(resultSet, "expires_at"),
                "usedAt", instant(resultSet, "used_at"),
                "dateCreation", instant(resultSet, "date_creation")
            )
        );
    }

    public List<Map<String, Object>> findBasicRights() {
        return jdbcTemplate.query(
            """
                SELECT id, code, br_name
                FROM gu.basic_rights
                ORDER BY code ASC, id ASC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "code", resultSet.getString("code"),
                "name", resultSet.getString("br_name")
            )
        );
    }

    public List<Map<String, Object>> findTypeUserBasicRights() {
        return jdbcTemplate.query(
            """
                SELECT
                    tubr.type_utilisateur_id,
                    tu.code AS type_code,
                    tubr.basic_right_id,
                    br.code AS basic_right_code,
                    tubr."dateCreation" AS date_creation
                FROM gu.type_utilisateur_basic_right tubr
                INNER JOIN gu.type_utilisateur tu ON tu.id = tubr.type_utilisateur_id
                INNER JOIN gu.basic_rights br ON br.id = tubr.basic_right_id
                ORDER BY tu.code ASC, br.code ASC
                """,
            (resultSet, rowNumber) -> {
                long typeUtilisateurId = resultSet.getLong("type_utilisateur_id");
                long basicRightId = resultSet.getLong("basic_right_id");
                return row(
                    "recordId", typeUtilisateurId + ":" + basicRightId,
                    "typeUtilisateurId", typeUtilisateurId,
                    "typeCode", resultSet.getString("type_code"),
                    "basicRightId", basicRightId,
                    "basicRightCode", resultSet.getString("basic_right_code"),
                    "dateCreation", instant(resultSet, "date_creation")
                );
            }
        );
    }

    public List<Map<String, Object>> findPasswordHistory() {
        return jdbcTemplate.query(
            """
                SELECT
                    ph.id,
                    ph.utilisateur_id,
                    u.login AS utilisateur_login,
                    u.email AS utilisateur_email,
                    ph."current" AS current_password,
                    ph.date_insertion,
                    ph.date_changement
                FROM gu.password_history ph
                INNER JOIN gu.utilisateurs u ON u.id = ph.utilisateur_id
                ORDER BY ph.date_insertion DESC, ph.id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "utilisateurId", resultSet.getLong("utilisateur_id"),
                "utilisateurLogin", resultSet.getString("utilisateur_login"),
                "utilisateurEmail", resultSet.getString("utilisateur_email"),
                "current", resultSet.getBoolean("current_password"),
                "dateInsertion", instant(resultSet, "date_insertion"),
                "dateChangement", instant(resultSet, "date_changement")
            )
        );
    }

    public Optional<AdminUserTypeRecord> findUserType(long id) {
        List<AdminUserTypeRecord> types = jdbcTemplate.query(
            "SELECT id, code, tu_name FROM gu.type_utilisateur WHERE id = ?",
            (resultSet, rowNumber) -> new AdminUserTypeRecord(
                resultSet.getLong("id"),
                resultSet.getString("code"),
                resultSet.getString("tu_name")
            ),
            id
        );
        return types.stream().findFirst();
    }

    public long insertUserType(String code, String name) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.type_utilisateur (code, tu_name)
                VALUES (?, ?)
                RETURNING id
                """,
            Long.class,
            code,
            name
        );
        return requiredId(id, "user type");
    }

    public boolean updateUserType(long id, String code, String name) {
        return jdbcTemplate.update(
            "UPDATE gu.type_utilisateur SET code = ?, tu_name = ? WHERE id = ?",
            code,
            name,
            id
        ) == 1;
    }

    public boolean userTypeHasUtilisateurs(long id) {
        return exists("SELECT EXISTS (SELECT 1 FROM gu.utilisateurs WHERE type_utilisateur_id = ?)", id);
    }

    public boolean userTypeHasBasicRights(long id) {
        return exists("SELECT EXISTS (SELECT 1 FROM gu.type_utilisateur_basic_right WHERE type_utilisateur_id = ?)", id);
    }

    public boolean deleteUserType(long id) {
        return jdbcTemplate.update("DELETE FROM gu.type_utilisateur WHERE id = ?", id) == 1;
    }

    public Optional<AdminUserRecord> findUtilisateur(long id) {
        List<AdminUserRecord> users = jdbcTemplate.query(
            """
                SELECT
                    u.id,
                    u.type_utilisateur_id,
                    tu.code AS type_code,
                    u.nom,
                    u.prenom,
                    u.email,
                    u.login,
                    u.statut
                FROM gu.utilisateurs u
                INNER JOIN gu.type_utilisateur tu ON tu.id = u.type_utilisateur_id
                WHERE u.id = ?
                """,
            (resultSet, rowNumber) -> new AdminUserRecord(
                resultSet.getLong("id"),
                resultSet.getLong("type_utilisateur_id"),
                resultSet.getString("type_code"),
                resultSet.getString("nom"),
                resultSet.getString("prenom"),
                resultSet.getString("email"),
                resultSet.getString("login"),
                resultSet.getString("statut")
            ),
            id
        );
        return users.stream().findFirst();
    }

    public boolean identityExists(String email, String login, Long excludingUtilisateurId) {
        Boolean exists = excludingUtilisateurId == null
            ? jdbcTemplate.queryForObject(
                """
                    SELECT EXISTS (
                        SELECT 1
                        FROM gu.utilisateurs
                        WHERE LOWER(email) = LOWER(?) OR LOWER(login) = LOWER(?)
                    )
                    """,
                Boolean.class,
                email,
                login
            )
            : jdbcTemplate.queryForObject(
                """
                    SELECT EXISTS (
                        SELECT 1
                        FROM gu.utilisateurs
                        WHERE (LOWER(email) = LOWER(?) OR LOWER(login) = LOWER(?))
                          AND id <> ?
                    )
                    """,
                Boolean.class,
                email,
                login,
                excludingUtilisateurId
            );
        return Boolean.TRUE.equals(exists);
    }

    public long insertUtilisateur(
        long userTypeId,
        String nom,
        String prenom,
        String email,
        String login,
        String statut,
        Instant createdAt
    ) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.utilisateurs
                    (type_utilisateur_id, nom, prenom, email, login, statut, "dateCreation")
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
            Long.class,
            userTypeId,
            nom,
            prenom,
            email,
            login,
            statut,
            Timestamp.from(createdAt)
        );
        return requiredId(id, "utilisateur");
    }

    public boolean updateUtilisateur(
        long id,
        long userTypeId,
        String nom,
        String prenom,
        String email,
        String login,
        String statut
    ) {
        return jdbcTemplate.update(
            """
                UPDATE gu.utilisateurs
                SET type_utilisateur_id = ?,
                    nom = ?,
                    prenom = ?,
                    email = ?,
                    login = ?,
                    statut = ?
                WHERE id = ?
                """,
            userTypeId,
            nom,
            prenom,
            email,
            login,
            statut,
            id
        ) == 1;
    }

    public boolean enterpriseIdentifiersExist(String niu, String rccm, long excludingUtilisateurId) {
        return exists(
            """
                SELECT EXISTS (
                    SELECT 1
                    FROM gu.client_entreprise
                    WHERE utilisateur_id <> ?
                      AND (UPPER(niu) = UPPER(?) OR UPPER(rccm) = UPPER(?))
                )
                """,
            excludingUtilisateurId,
            niu,
            rccm
        );
    }

    public boolean updateClientEntreprise(long utilisateurId, String raisonSociale, String niu, String rccm) {
        return jdbcTemplate.update(
            """
                UPDATE gu.client_entreprise
                SET raison_sociale = ?,
                    niu = ?,
                    rccm = ?
                WHERE utilisateur_id = ?
                """,
            raisonSociale,
            niu,
            rccm,
            utilisateurId
        ) == 1;
    }

    public boolean utilisateurHasAppConnection(long userTypeId) {
        return exists(
            """
                SELECT EXISTS (
                    SELECT 1
                    FROM gu.type_utilisateur_basic_right tubr
                    INNER JOIN gu.basic_rights br ON br.id = tubr.basic_right_id
                    WHERE tubr.type_utilisateur_id = ?
                      AND br.code = 'APP-CONN'
                )
                """,
            userTypeId
        );
    }

    /**
     * Serializes destructive/suspending administrator changes so two concurrent requests cannot
     * each observe two administrators and leave the system without any active administrator.
     */
    public void lockActiveAdministrators() {
        jdbcTemplate.query(
            """
                SELECT u.id
                FROM gu.utilisateurs u
                INNER JOIN gu.type_utilisateur tu ON tu.id = u.type_utilisateur_id
                WHERE tu.code = 'ADMINISTRATEUR'
                  AND u.statut = 'ACTIF'
                ORDER BY u.id ASC
                FOR UPDATE
                """,
            resultSet -> {
                // Acquiring the row locks is the purpose of this query.
            }
        );
    }

    public long countActiveAdministrators() {
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM gu.utilisateurs u
                INNER JOIN gu.type_utilisateur tu ON tu.id = u.type_utilisateur_id
                WHERE tu.code = 'ADMINISTRATEUR'
                  AND u.statut = 'ACTIF'
                """,
            Long.class
        );
        return count == null ? 0 : count;
    }

    public void insertPasswordHash(long utilisateurId, String hash, Instant insertedAt) {
        jdbcTemplate.update(
            """
                INSERT INTO gu.password_history (utilisateur_id, "password", "current", date_insertion)
                VALUES (?, ?, TRUE, ?)
                """,
            utilisateurId,
            hash,
            Timestamp.from(insertedAt)
        );
    }

    public void deletePasswordHistory(long utilisateurId) {
        jdbcTemplate.update("DELETE FROM gu.password_history WHERE utilisateur_id = ?", utilisateurId);
    }

    public boolean deleteUtilisateur(long utilisateurId) {
        return jdbcTemplate.update("DELETE FROM gu.utilisateurs WHERE id = ?", utilisateurId) == 1;
    }

    public boolean revokeSession(long id, Instant revokedAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.sessions_utilisateur
                SET invalidated_at = ?
                WHERE id = ?
                  AND invalidated_at IS NULL
                """,
            Timestamp.from(revokedAt),
            id
        ) == 1;
    }

    public Optional<AdminConfirmationRecord> findRegistrationConfirmation(long id) {
        List<AdminConfirmationRecord> records = jdbcTemplate.query(
            """
                SELECT rc.utilisateur_id, u.statut, rc.confirmed_at
                FROM gu.registration_confirmation rc
                INNER JOIN gu.utilisateurs u ON u.id = rc.utilisateur_id
                WHERE rc.id = ?
                """,
            (resultSet, rowNumber) -> new AdminConfirmationRecord(
                resultSet.getLong("utilisateur_id"),
                resultSet.getString("statut"),
                instant(resultSet, "confirmed_at")
            ),
            id
        );
        return records.stream().findFirst();
    }

    public boolean deletePasswordReset(long id) {
        return jdbcTemplate.update("DELETE FROM gu.password_reset WHERE id = ?", id) == 1;
    }

    public boolean basicRightExists(long id) {
        return exists("SELECT EXISTS (SELECT 1 FROM gu.basic_rights WHERE id = ?)", id);
    }

    public Optional<String> findBasicRightCode(long id) {
        List<String> codes = jdbcTemplate.query(
            "SELECT code FROM gu.basic_rights WHERE id = ?",
            (resultSet, rowNumber) -> resultSet.getString("code"),
            id
        );
        return codes.stream().findFirst();
    }

    public long insertBasicRight(String code, String name) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.basic_rights (code, br_name)
                VALUES (?, ?)
                RETURNING id
                """,
            Long.class,
            code,
            name
        );
        return requiredId(id, "basic right");
    }

    public boolean updateBasicRight(long id, String code, String name) {
        return jdbcTemplate.update(
            "UPDATE gu.basic_rights SET code = ?, br_name = ? WHERE id = ?",
            code,
            name,
            id
        ) == 1;
    }

    public boolean insertTypeUserBasicRight(long typeUtilisateurId, long basicRightId, Instant createdAt) {
        return jdbcTemplate.update(
            """
                INSERT INTO gu.type_utilisateur_basic_right
                    (type_utilisateur_id, basic_right_id, "dateCreation")
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
                """,
            typeUtilisateurId,
            basicRightId,
            Timestamp.from(createdAt)
        ) == 1;
    }

    public boolean deleteTypeUserBasicRight(long typeUtilisateurId, long basicRightId) {
        return jdbcTemplate.update(
            """
                DELETE FROM gu.type_utilisateur_basic_right
                WHERE type_utilisateur_id = ?
                  AND basic_right_id = ?
                """,
            typeUtilisateurId,
            basicRightId
        ) == 1;
    }

    private boolean exists(String sql, Object... parameters) {
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, parameters);
        return Boolean.TRUE.equals(exists);
    }

    private static long requiredId(Long id, String resource) {
        if (id == null) {
            throw new IllegalStateException("The database did not return an identifier for " + resource + ".");
        }
        return id;
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Map<String, Object> row(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            row.put((String) values[index], values[index + 1]);
        }
        return row;
    }
}
