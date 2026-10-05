package cm.odigital.serviceconnectmarket.market.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * SQL layer of the cocoa catalogue and of the buyer/seller conversations.
 *
 * <p>Like the administrator repository, every statement is fixed here and every value is bound as
 * a parameter: request data never becomes a table name, a column name, or a SQL fragment. The
 * catalogue filters append a constant fragment chosen by the code, never by the caller.
 */
@Repository
public class MarketRepository {

    private static final String LOT_PROJECTION = """
        SELECT
            l.id,
            l.vendeur_id,
            l.type_cacao_id,
            l.titre,
            l.description,
            l.quantite_kg,
            l.quantite_disponible_kg,
            l.prix_kg,
            l.devise,
            l.region_id,
            l.ville_id,
            l.localisation,
            l.latitude,
            l.longitude,
            l.date_recolte,
            l.date_disponibilite,
            l.statut,
            l.date_creation,
            l.date_publication,
            l.date_mise_a_jour,
            tc.code AS type_code,
            tc.nom AS type_nom,
            r.code AS region_code,
            r.nom AS region_nom,
            v.nom AS ville_nom,
            u.prenom AS vendeur_prenom,
            u.nom AS vendeur_nom,
            u.login AS vendeur_login,
            (
                SELECT m.url
                FROM gu.lot_medias m
                WHERE m.lot_id = l.id
                ORDER BY m.position ASC, m.id ASC
                LIMIT 1
            ) AS photo_url
        FROM gu.lots l
        INNER JOIN gu.type_cacao tc ON tc.id = l.type_cacao_id
        INNER JOIN gu.region r ON r.id = l.region_id
        LEFT JOIN gu.ville v ON v.id = l.ville_id
        INNER JOIN gu.utilisateurs u ON u.id = l.vendeur_id
        """;

    private static final String CONVERSATION_PROJECTION = """
        SELECT
            c.id,
            c.lot_id,
            c.client_id,
            c.vendeur_id,
            c.statut,
            c.date_creation,
            c.dernier_message_at,
            l.titre AS lot_titre,
            l.prix_kg AS lot_prix_kg,
            l.devise AS lot_devise,
            l.statut AS lot_statut,
            l.quantite_disponible_kg AS lot_quantite_disponible_kg,
            cl.prenom AS client_prenom,
            cl.nom AS client_nom,
            cl.login AS client_login,
            ve.prenom AS vendeur_prenom,
            ve.nom AS vendeur_nom,
            ve.login AS vendeur_login,
            (
                SELECT m.contenu
                FROM gu.messages m
                WHERE m.conversation_id = c.id
                ORDER BY m.date_envoi DESC, m.id DESC
                LIMIT 1
            ) AS dernier_message,
            (
                SELECT COUNT(*)
                FROM gu.messages m
                WHERE m.conversation_id = c.id
                  AND m.expediteur_id <> ?
                  AND m.lu_at IS NULL
            ) AS non_lus
        FROM gu.conversations c
        INNER JOIN gu.lots l ON l.id = c.lot_id
        INNER JOIN gu.utilisateurs cl ON cl.id = c.client_id
        INNER JOIN gu.utilisateurs ve ON ve.id = c.vendeur_id
        """;

    private final JdbcTemplate jdbcTemplate;

    public MarketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------------ reference data

    public List<Map<String, Object>> findRegions() {
        return jdbcTemplate.query(
            "SELECT id, code, nom FROM gu.region ORDER BY nom ASC",
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "code", resultSet.getString("code"),
                "nom", resultSet.getString("nom")
            )
        );
    }

    public List<Map<String, Object>> findVilles() {
        return jdbcTemplate.query(
            """
                SELECT v.id, v.region_id, v.nom
                FROM gu.ville v
                INNER JOIN gu.region r ON r.id = v.region_id
                ORDER BY r.nom ASC, v.nom ASC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "regionId", resultSet.getLong("region_id"),
                "nom", resultSet.getString("nom")
            )
        );
    }

    public List<Map<String, Object>> findCacaoTypes() {
        return jdbcTemplate.query(
            "SELECT id, code, nom, description FROM gu.type_cacao ORDER BY nom ASC",
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "code", resultSet.getString("code"),
                "nom", resultSet.getString("nom"),
                "description", resultSet.getString("description")
            )
        );
    }

    public boolean regionExists(long regionId) {
        return exists("SELECT EXISTS (SELECT 1 FROM gu.region WHERE id = ?)", regionId);
    }

    public boolean villeBelongsToRegion(long villeId, long regionId) {
        return exists(
            "SELECT EXISTS (SELECT 1 FROM gu.ville WHERE id = ? AND region_id = ?)",
            villeId,
            regionId
        );
    }

    public boolean cacaoTypeExists(long typeCacaoId) {
        return exists("SELECT EXISTS (SELECT 1 FROM gu.type_cacao WHERE id = ?)", typeCacaoId);
    }

    // ------------------------------------------------------------------ lots

    public List<Map<String, Object>> findCatalogue(LotFilter filter) {
        StringBuilder sql = new StringBuilder(LOT_PROJECTION);
        List<Object> parameters = new ArrayList<>();
        sql.append(" WHERE l.statut IN ('PUBLIE', 'RESERVE')");

        if (filter.regionId() != null) {
            sql.append(" AND l.region_id = ?");
            parameters.add(filter.regionId());
        }
        if (filter.villeId() != null) {
            sql.append(" AND l.ville_id = ?");
            parameters.add(filter.villeId());
        }
        if (filter.typeCacaoId() != null) {
            sql.append(" AND l.type_cacao_id = ?");
            parameters.add(filter.typeCacaoId());
        }
        if (filter.recolteFrom() != null) {
            sql.append(" AND l.date_recolte >= ?");
            parameters.add(java.sql.Date.valueOf(filter.recolteFrom()));
        }
        if (filter.recolteTo() != null) {
            sql.append(" AND l.date_recolte <= ?");
            parameters.add(java.sql.Date.valueOf(filter.recolteTo()));
        }
        if (filter.disponibiliteFrom() != null) {
            sql.append(" AND l.date_disponibilite >= ?");
            parameters.add(java.sql.Date.valueOf(filter.disponibiliteFrom()));
        }
        if (filter.prixMax() != null) {
            sql.append(" AND l.prix_kg <= ?");
            parameters.add(filter.prixMax());
        }
        if (filter.prixMin() != null) {
            sql.append(" AND l.prix_kg >= ?");
            parameters.add(filter.prixMin());
        }
        if (filter.quantiteMin() != null) {
            sql.append(" AND l.quantite_disponible_kg >= ?");
            parameters.add(filter.quantiteMin());
        }
        if (filter.search() != null) {
            sql.append("""
                 AND (
                     l.titre ILIKE ?
                     OR COALESCE(l.description, '') ILIKE ?
                     OR COALESCE(l.localisation, '') ILIKE ?
                     OR r.nom ILIKE ?
                     OR COALESCE(v.nom, '') ILIKE ?
                 )
                """);
            String pattern = "%" + filter.search() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }

        sql.append(" ORDER BY l.date_publication DESC NULLS LAST, l.id DESC");
        return jdbcTemplate.query(sql.toString(), lotRowMapper(), parameters.toArray());
    }

    public List<Map<String, Object>> findLotsForVendeur(long vendeurId) {
        return jdbcTemplate.query(
            LOT_PROJECTION + " WHERE l.vendeur_id = ? ORDER BY l.date_creation DESC, l.id DESC",
            lotRowMapper(),
            vendeurId
        );
    }

    public Optional<Map<String, Object>> findLot(long lotId) {
        List<Map<String, Object>> lots = jdbcTemplate.query(
            LOT_PROJECTION + " WHERE l.id = ?",
            lotRowMapper(),
            lotId
        );
        return lots.stream().findFirst();
    }

    public List<Map<String, Object>> findLotMedias(long lotId) {
        return jdbcTemplate.query(
            """
                SELECT id, url, legende, position
                FROM gu.lot_medias
                WHERE lot_id = ?
                ORDER BY position ASC, id ASC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "url", resultSet.getString("url"),
                "legende", resultSet.getString("legende"),
                "position", resultSet.getInt("position")
            ),
            lotId
        );
    }

    public long insertLot(
        long vendeurId,
        long typeCacaoId,
        String titre,
        String description,
        BigDecimal quantiteKg,
        BigDecimal prixKg,
        String devise,
        long regionId,
        Long villeId,
        String localisation,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate dateRecolte,
        LocalDate dateDisponibilite,
        String statut,
        Instant publicationAt,
        Instant now
    ) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.lots (
                    vendeur_id, type_cacao_id, titre, description,
                    quantite_kg, quantite_disponible_kg, prix_kg, devise,
                    region_id, ville_id, localisation, latitude, longitude,
                    date_recolte, date_disponibilite, statut,
                    date_creation, date_publication, date_mise_a_jour
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
            Long.class,
            vendeurId,
            typeCacaoId,
            titre,
            description,
            quantiteKg,
            quantiteKg,
            prixKg,
            devise,
            regionId,
            villeId,
            localisation,
            latitude,
            longitude,
            dateRecolte == null ? null : java.sql.Date.valueOf(dateRecolte),
            dateDisponibilite == null ? null : java.sql.Date.valueOf(dateDisponibilite),
            statut,
            Timestamp.from(now),
            publicationAt == null ? null : Timestamp.from(publicationAt),
            Timestamp.from(now)
        );
        return id == null ? 0L : id;
    }

    public boolean updateLot(
        long lotId,
        long vendeurId,
        long typeCacaoId,
        String titre,
        String description,
        BigDecimal quantiteKg,
        BigDecimal prixKg,
        String devise,
        long regionId,
        Long villeId,
        String localisation,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate dateRecolte,
        LocalDate dateDisponibilite,
        Instant now
    ) {
        return jdbcTemplate.update(
            """
                UPDATE gu.lots
                SET type_cacao_id = ?,
                    titre = ?,
                    description = ?,
                    quantite_kg = ?,
                    quantite_disponible_kg = LEAST(quantite_disponible_kg, ?),
                    prix_kg = ?,
                    devise = ?,
                    region_id = ?,
                    ville_id = ?,
                    localisation = ?,
                    latitude = ?,
                    longitude = ?,
                    date_recolte = ?,
                    date_disponibilite = ?,
                    date_mise_a_jour = ?
                WHERE id = ?
                  AND vendeur_id = ?
                """,
            typeCacaoId,
            titre,
            description,
            quantiteKg,
            quantiteKg,
            prixKg,
            devise,
            regionId,
            villeId,
            localisation,
            latitude,
            longitude,
            dateRecolte == null ? null : java.sql.Date.valueOf(dateRecolte),
            dateDisponibilite == null ? null : java.sql.Date.valueOf(dateDisponibilite),
            Timestamp.from(now),
            lotId,
            vendeurId
        ) == 1;
    }

    public boolean updateLotStatus(long lotId, long vendeurId, String statut, Instant publicationAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.lots
                SET statut = ?,
                    date_publication = COALESCE(?, date_publication),
                    date_mise_a_jour = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND vendeur_id = ?
                """,
            statut,
            publicationAt == null ? null : Timestamp.from(publicationAt),
            lotId,
            vendeurId
        ) == 1;
    }

    /** Reserves a published lot after an accepted negotiation. */
    public boolean reserveLot(long lotId) {
        return jdbcTemplate.update(
            "UPDATE gu.lots SET statut = 'RESERVE', date_mise_a_jour = CURRENT_TIMESTAMP WHERE id = ? AND statut = 'PUBLIE'",
            lotId
        ) == 1;
    }

    /** Removes the agreed volume from a lot; the guard refuses an over-commitment. */
    public boolean reduceAvailableQuantity(long lotId, BigDecimal quantiteKg) {
        return jdbcTemplate.update(
            """
                UPDATE gu.lots
                SET quantite_disponible_kg = quantite_disponible_kg - ?,
                    date_mise_a_jour = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND quantite_disponible_kg >= ?
                """,
            quantiteKg,
            lotId,
            quantiteKg
        ) == 1;
    }

    public void deleteLotMedias(long lotId) {
        jdbcTemplate.update("DELETE FROM gu.lot_medias WHERE lot_id = ?", lotId);
    }

    public void insertLotMedia(long lotId, String url, String legende, int position) {
        jdbcTemplate.update(
            "INSERT INTO gu.lot_medias (lot_id, url, legende, position) VALUES (?, ?, ?, ?)",
            lotId,
            url,
            legende,
            position
        );
    }

    // ------------------------------------------------------------------ conversations

    public Optional<Map<String, Object>> findConversationIdByLotAndClient(long lotId, long clientId) {
        List<Map<String, Object>> conversations = jdbcTemplate.query(
            "SELECT id, statut FROM gu.conversations WHERE lot_id = ? AND client_id = ?",
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "statut", resultSet.getString("statut")
            ),
            lotId,
            clientId
        );
        return conversations.stream().findFirst();
    }

    public long insertConversation(long lotId, long clientId, long vendeurId) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.conversations (lot_id, client_id, vendeur_id)
                VALUES (?, ?, ?)
                RETURNING id
                """,
            Long.class,
            lotId,
            clientId,
            vendeurId
        );
        return id == null ? 0L : id;
    }

    public Optional<Map<String, Object>> findConversation(long conversationId, long readerId) {
        List<Map<String, Object>> conversations = jdbcTemplate.query(
            CONVERSATION_PROJECTION + " WHERE c.id = ?",
            conversationRowMapper(readerId),
            readerId,
            conversationId
        );
        return conversations.stream().findFirst();
    }

    public List<Map<String, Object>> findConversationsForClient(long clientId) {
        return jdbcTemplate.query(
            CONVERSATION_PROJECTION + " WHERE c.client_id = ? ORDER BY COALESCE(c.dernier_message_at, c.date_creation) DESC, c.id DESC",
            conversationRowMapper(clientId),
            clientId,
            clientId
        );
    }

    public List<Map<String, Object>> findConversationsForVendeur(long vendeurId) {
        return jdbcTemplate.query(
            CONVERSATION_PROJECTION + " WHERE c.vendeur_id = ? ORDER BY COALESCE(c.dernier_message_at, c.date_creation) DESC, c.id DESC",
            conversationRowMapper(vendeurId),
            vendeurId,
            vendeurId
        );
    }

    public boolean updateConversationStatus(long conversationId, String statut) {
        return jdbcTemplate.update(
            "UPDATE gu.conversations SET statut = ? WHERE id = ? AND statut <> ?",
            statut,
            conversationId,
            statut
        ) == 1;
    }

    public List<Map<String, Object>> findMessages(long conversationId) {
        return jdbcTemplate.query(
            """
                SELECT id, expediteur_id, contenu, type, date_envoi, lu_at
                FROM gu.messages
                WHERE conversation_id = ?
                ORDER BY date_envoi ASC, id ASC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "expediteurId", resultSet.getLong("expediteur_id"),
                "contenu", resultSet.getString("contenu"),
                "type", resultSet.getString("type"),
                "dateEnvoi", instant(resultSet, "date_envoi"),
                "luAt", instant(resultSet, "lu_at")
            ),
            conversationId
        );
    }

    public void insertMessage(long conversationId, long expediteurId, String contenu, String type, Instant now) {
        jdbcTemplate.update(
            """
                INSERT INTO gu.messages (conversation_id, expediteur_id, contenu, type, date_envoi)
                VALUES (?, ?, ?, ?, ?)
                """,
            conversationId,
            expediteurId,
            contenu,
            type,
            Timestamp.from(now)
        );
    }

    public void markMessagesRead(long conversationId, long readerId, Instant now) {
        jdbcTemplate.update(
            """
                UPDATE gu.messages
                SET lu_at = ?
                WHERE conversation_id = ?
                  AND expediteur_id <> ?
                  AND lu_at IS NULL
                """,
            Timestamp.from(now),
            conversationId,
            readerId
        );
    }

    // ------------------------------------------------------------------ negotiations

    public boolean hasOpenNegociation(long conversationId) {
        return exists(
            "SELECT EXISTS (SELECT 1 FROM gu.negociations WHERE conversation_id = ? AND statut = 'PROPOSEE')",
            conversationId
        );
    }

    public long insertNegociation(
        long conversationId,
        long proposeurId,
        BigDecimal prixKg,
        BigDecimal quantiteKg,
        String message,
        Instant expiresAt,
        Instant now
    ) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.negociations (
                    conversation_id, proposeur_id, prix_kg, quantite_kg, message, expires_at, date_creation
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
            Long.class,
            conversationId,
            proposeurId,
            prixKg,
            quantiteKg,
            message,
            Timestamp.from(expiresAt),
            Timestamp.from(now)
        );
        return id == null ? 0L : id;
    }

    public Optional<Map<String, Object>> findNegociation(long negociationId) {
        List<Map<String, Object>> negociation = jdbcTemplate.query(
            """
                SELECT id, conversation_id, proposeur_id, prix_kg, quantite_kg, message, statut, expires_at
                FROM gu.negociations
                WHERE id = ?
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "conversationId", resultSet.getLong("conversation_id"),
                "proposeurId", resultSet.getLong("proposeur_id"),
                "prixKg", resultSet.getBigDecimal("prix_kg"),
                "quantiteKg", resultSet.getBigDecimal("quantite_kg"),
                "message", resultSet.getString("message"),
                "statut", resultSet.getString("statut"),
                "expiresAt", instant(resultSet, "expires_at")
            ),
            negociationId
        );
        return negociation.stream().findFirst();
    }

    public List<Map<String, Object>> findNegociations(long conversationId) {
        return jdbcTemplate.query(
            """
                SELECT id, proposeur_id, prix_kg, quantite_kg, message, statut, date_creation, date_reponse, expires_at
                FROM gu.negociations
                WHERE conversation_id = ?
                ORDER BY date_creation DESC, id DESC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "proposeurId", resultSet.getLong("proposeur_id"),
                "prixKg", resultSet.getBigDecimal("prix_kg"),
                "quantiteKg", resultSet.getBigDecimal("quantite_kg"),
                "message", resultSet.getString("message"),
                "statut", resultSet.getString("statut"),
                "dateCreation", instant(resultSet, "date_creation"),
                "dateReponse", instant(resultSet, "date_reponse"),
                "expiresAt", instant(resultSet, "expires_at")
            ),
            conversationId
        );
    }

    public boolean updateNegociationStatus(long negociationId, String statut, Instant respondedAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.negociations
                SET statut = ?,
                    date_reponse = ?
                WHERE id = ?
                  AND statut = 'PROPOSEE'
                """,
            statut,
            Timestamp.from(respondedAt),
            negociationId
        ) == 1;
    }

    /**
     * Marks every overdue proposal as expired and returns the affected conversations so their
     * status can be brought back to OUVERTE. Called before reading a thread, which keeps the
     * expiry rule visible without a background scheduler.
     */
    public List<Long> expireOverdueNegociations(Instant now) {
        return jdbcTemplate.query(
            """
                UPDATE gu.negociations
                SET statut = 'EXPIREE',
                    date_reponse = ?
                WHERE statut = 'PROPOSEE'
                  AND expires_at IS NOT NULL
                  AND expires_at <= ?
                RETURNING conversation_id
                """,
            (resultSet, rowNumber) -> resultSet.getLong("conversation_id"),
            Timestamp.from(now),
            Timestamp.from(now)
        );
    }

    // ------------------------------------------------------------------ visits (rendez-vous)

    public boolean hasOpenRendezVous(long conversationId) {
        return exists(
            "SELECT EXISTS (SELECT 1 FROM gu.rendez_vous WHERE conversation_id = ? AND statut = 'PROPOSE')",
            conversationId
        );
    }

    public long insertRendezVous(
        long conversationId,
        long proposeurId,
        Instant dateProposee,
        String lieu,
        String note,
        Instant now
    ) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO gu.rendez_vous (
                    conversation_id, proposeur_id, date_proposee, lieu, note, date_creation
                )
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
            Long.class,
            conversationId,
            proposeurId,
            Timestamp.from(dateProposee),
            lieu,
            note,
            Timestamp.from(now)
        );
        return id == null ? 0L : id;
    }

    public Optional<Map<String, Object>> findRendezVous(long rendezVousId) {
        List<Map<String, Object>> rendezVous = jdbcTemplate.query(
            """
                SELECT id, conversation_id, proposeur_id, date_proposee, lieu, note, statut
                FROM gu.rendez_vous
                WHERE id = ?
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "conversationId", resultSet.getLong("conversation_id"),
                "proposeurId", resultSet.getLong("proposeur_id"),
                "dateProposee", instant(resultSet, "date_proposee"),
                "lieu", resultSet.getString("lieu"),
                "note", resultSet.getString("note"),
                "statut", resultSet.getString("statut")
            ),
            rendezVousId
        );
        return rendezVous.stream().findFirst();
    }

    public List<Map<String, Object>> findRendezVousList(long conversationId) {
        return jdbcTemplate.query(
            """
                SELECT id, proposeur_id, date_proposee, lieu, note, statut, date_creation, date_reponse
                FROM gu.rendez_vous
                WHERE conversation_id = ?
                ORDER BY date_proposee ASC, id ASC
                """,
            (resultSet, rowNumber) -> row(
                "id", resultSet.getLong("id"),
                "proposeurId", resultSet.getLong("proposeur_id"),
                "dateProposee", instant(resultSet, "date_proposee"),
                "lieu", resultSet.getString("lieu"),
                "note", resultSet.getString("note"),
                "statut", resultSet.getString("statut"),
                "dateCreation", instant(resultSet, "date_creation"),
                "dateReponse", instant(resultSet, "date_reponse")
            ),
            conversationId
        );
    }

    public boolean updateRendezVousStatus(long rendezVousId, String statut, Instant respondedAt) {
        return jdbcTemplate.update(
            """
                UPDATE gu.rendez_vous
                SET statut = ?,
                    date_reponse = ?
                WHERE id = ?
                  AND statut = 'PROPOSE'
                """,
            statut,
            Timestamp.from(respondedAt),
            rendezVousId
        ) == 1;
    }

    // ------------------------------------------------------------------ helpers

    public Optional<String> findUtilisateurRole(long utilisateurId) {
        List<String> roles = jdbcTemplate.query(
            """
                SELECT tu.code
                FROM gu.utilisateurs u
                INNER JOIN gu.type_utilisateur tu ON tu.id = u.type_utilisateur_id
                WHERE u.id = ?
                """,
            (resultSet, rowNumber) -> resultSet.getString("code"),
            utilisateurId
        );
        return roles.stream().findFirst();
    }

    private boolean exists(String sql, Object... parameters) {
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, parameters);
        return Boolean.TRUE.equals(exists);
    }

    private static RowMapper<Map<String, Object>> lotRowMapper() {
        return (resultSet, rowNumber) -> row(
            "id", resultSet.getLong("id"),
            "vendeurId", resultSet.getLong("vendeur_id"),
            "typeCacaoId", resultSet.getLong("type_cacao_id"),
            "titre", resultSet.getString("titre"),
            "description", resultSet.getString("description"),
            "quantiteKg", resultSet.getBigDecimal("quantite_kg"),
            "quantiteDisponibleKg", resultSet.getBigDecimal("quantite_disponible_kg"),
            "prixKg", resultSet.getBigDecimal("prix_kg"),
            "devise", resultSet.getString("devise"),
            "regionId", resultSet.getLong("region_id"),
            "villeId", resultSet.getObject("ville_id") == null ? null : resultSet.getLong("ville_id"),
            "localisation", resultSet.getString("localisation"),
            "latitude", resultSet.getBigDecimal("latitude"),
            "longitude", resultSet.getBigDecimal("longitude"),
            "dateRecolte", localDate(resultSet, "date_recolte"),
            "dateDisponibilite", localDate(resultSet, "date_disponibilite"),
            "statut", resultSet.getString("statut"),
            "dateCreation", instant(resultSet, "date_creation"),
            "datePublication", instant(resultSet, "date_publication"),
            "dateMiseAJour", instant(resultSet, "date_mise_a_jour"),
            "typeCode", resultSet.getString("type_code"),
            "typeNom", resultSet.getString("type_nom"),
            "regionCode", resultSet.getString("region_code"),
            "regionNom", resultSet.getString("region_nom"),
            "villeNom", resultSet.getString("ville_nom"),
            "vendeurPrenom", resultSet.getString("vendeur_prenom"),
            "vendeurNom", resultSet.getString("vendeur_nom"),
            "vendeurLogin", resultSet.getString("vendeur_login"),
            "photoUrl", resultSet.getString("photo_url")
        );
    }

    private static RowMapper<Map<String, Object>> conversationRowMapper(long readerId) {
        return (resultSet, rowNumber) -> row(
            "id", resultSet.getLong("id"),
            "lotId", resultSet.getLong("lot_id"),
            "clientId", resultSet.getLong("client_id"),
            "vendeurId", resultSet.getLong("vendeur_id"),
            "statut", resultSet.getString("statut"),
            "dateCreation", instant(resultSet, "date_creation"),
            "dernierMessageAt", instant(resultSet, "dernier_message_at"),
            "lotTitre", resultSet.getString("lot_titre"),
            "lotPrixKg", resultSet.getBigDecimal("lot_prix_kg"),
            "lotDevise", resultSet.getString("lot_devise"),
            "lotStatut", resultSet.getString("lot_statut"),
            "lotQuantiteDisponibleKg", resultSet.getBigDecimal("lot_quantite_disponible_kg"),
            "clientPrenom", resultSet.getString("client_prenom"),
            "clientNom", resultSet.getString("client_nom"),
            "clientLogin", resultSet.getString("client_login"),
            "vendeurPrenom", resultSet.getString("vendeur_prenom"),
            "vendeurNom", resultSet.getString("vendeur_nom"),
            "vendeurLogin", resultSet.getString("vendeur_login"),
            "dernierMessage", resultSet.getString("dernier_message"),
            "nonLus", resultSet.getLong("non_lus"),
            "lecteurId", readerId
        );
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static LocalDate localDate(ResultSet resultSet, String column) throws SQLException {
        java.sql.Date date = resultSet.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private static Map<String, Object> row(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            row.put((String) values[index], values[index + 1]);
        }
        return row;
    }

    /** Safe, code-owned filters of the catalogue query. */
    public record LotFilter(
        Long regionId,
        Long villeId,
        Long typeCacaoId,
        LocalDate recolteFrom,
        LocalDate recolteTo,
        LocalDate disponibiliteFrom,
        BigDecimal prixMin,
        BigDecimal prixMax,
        BigDecimal quantiteMin,
        String search
    ) {
    }
}
