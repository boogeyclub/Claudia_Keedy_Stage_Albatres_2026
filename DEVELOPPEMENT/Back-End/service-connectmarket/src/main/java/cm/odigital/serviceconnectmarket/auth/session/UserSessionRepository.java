package cm.odigital.serviceconnectmarket.auth.session;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;

@Repository
public class UserSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(
        long utilisateurId,
        String sessionHash,
        String browserLabel,
        boolean rememberMe,
        Instant now,
        Instant expiresAt
    ) {
        Long sessionId = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.sessions_utilisateur
                    (utilisateur_id, session_hash, browser_label, remember_me, date_creation, last_seen_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
            Long.class,
            utilisateurId,
            sessionHash,
            browserLabel,
            rememberMe,
            Timestamp.from(now),
            Timestamp.from(now),
            Timestamp.from(expiresAt)
        );
        if (sessionId == null) {
            throw new IllegalStateException("The database did not return a browser-session identifier.");
        }
        return sessionId;
    }

    public Optional<AuthenticatedSession> findActiveBySessionHash(String sessionHash, Instant now) {
        List<AuthenticatedSession> sessions = jdbcTemplate.query(
            """
                SELECT
                    s.id AS session_id,
                    u.id AS utilisateur_id,
                    u.email,
                    u.login,
                    u.prenom,
                    u.nom,
                    tu.code AS type_code
                FROM gu.sessions_utilisateur s
                INNER JOIN gu.utilisateurs u ON u.id = s.utilisateur_id
                INNER JOIN gu.type_utilisateur tu ON tu.id = u.type_utilisateur_id
                WHERE s.session_hash = ?
                  AND s.invalidated_at IS NULL
                  AND s.expires_at > ?
                  AND u.statut = 'ACTIF'
                  AND tu.code IN ('VENDEUR', 'CLIENT', 'ADMINISTRATEUR')
                  AND EXISTS (
                      SELECT 1
                      FROM gu.type_utilisateur_basic_right tubr
                      INNER JOIN gu.basic_rights br ON br.id = tubr.basic_right_id
                      WHERE tubr.type_utilisateur_id = tu.id
                        AND br.code = 'APP-CONN'
                  )
                LIMIT 1
                """,
            (resultSet, rowNumber) -> new AuthenticatedSession(
                resultSet.getLong("session_id"),
                new AuthenticatedUtilisateur(
                    resultSet.getLong("utilisateur_id"),
                    resultSet.getString("email"),
                    resultSet.getString("login"),
                    resultSet.getString("prenom"),
                    resultSet.getString("nom"),
                    resultSet.getString("type_code")
                )
            ),
            sessionHash,
            Timestamp.from(now)
        );
        return sessions.stream().findFirst();
    }

    public boolean touch(long sessionId, Instant now, Instant expiresAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.sessions_utilisateur
                SET last_seen_at = ?,
                    expires_at = ?
                WHERE id = ?
                  AND invalidated_at IS NULL
                  AND expires_at > ?
                """,
            Timestamp.from(now),
            Timestamp.from(expiresAt),
            sessionId,
            Timestamp.from(now)
        ) == 1;
    }

    public boolean revokeBySessionHash(String sessionHash, Instant revokedAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.sessions_utilisateur
                SET invalidated_at = ?
                WHERE session_hash = ?
                  AND invalidated_at IS NULL
                """,
            Timestamp.from(revokedAt),
            sessionHash
        ) == 1;
    }

    public boolean revokeOwnedSession(long sessionId, long utilisateurId, Instant revokedAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.sessions_utilisateur
                SET invalidated_at = ?
                WHERE id = ?
                  AND utilisateur_id = ?
                  AND invalidated_at IS NULL
                """,
            Timestamp.from(revokedAt),
            sessionId,
            utilisateurId
        ) == 1;
    }

    /**
     * Security-sensitive password changes invalidate every active browser cookie for that account.
     */
    public int revokeAllForUtilisateur(long utilisateurId, Instant revokedAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.sessions_utilisateur
                SET invalidated_at = ?
                WHERE utilisateur_id = ?
                  AND invalidated_at IS NULL
                """,
            Timestamp.from(revokedAt),
            utilisateurId
        );
    }

    public List<BrowserSession> findActiveForUtilisateur(long utilisateurId, Instant now) {
        return jdbcTemplate.query(
            """
                SELECT id, browser_label, remember_me, date_creation, last_seen_at, expires_at
                FROM gu.sessions_utilisateur
                WHERE utilisateur_id = ?
                  AND invalidated_at IS NULL
                  AND expires_at > ?
                ORDER BY last_seen_at DESC, id DESC
                """,
            (resultSet, rowNumber) -> new BrowserSession(
                resultSet.getLong("id"),
                resultSet.getString("browser_label"),
                resultSet.getBoolean("remember_me"),
                resultSet.getTimestamp("date_creation").toInstant(),
                resultSet.getTimestamp("last_seen_at").toInstant(),
                resultSet.getTimestamp("expires_at").toInstant()
            ),
            utilisateurId,
            Timestamp.from(now)
        );
    }

    public int invalidateExpiredSessions(Instant now) {
        return jdbcTemplate.update(
            """
                UPDATE gu.sessions_utilisateur
                SET invalidated_at = expires_at
                WHERE invalidated_at IS NULL
                  AND expires_at <= ?
                """,
            Timestamp.from(now)
        );
    }
}
