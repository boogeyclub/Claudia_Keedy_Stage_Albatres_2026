package cm.odigital.serviceconnectmarket.schema;

import java.util.List;

/**
 * Code-owned description of the PostgreSQL schema created by
 * {@code DEVELOPPEMENT/database/gu.sql}.
 *
 * <p>The startup verification and the database health endpoint both read this catalog instead of
 * scanning every table of the database. That keeps the check fast and deterministic, and no SQL
 * identifier is ever built from request data. The columns listed here are the ones the API actually
 * reads or writes, so a missing entry means the tracked script was not applied, or was applied from
 * an older revision.
 */
public final class GuSchemaCatalog {

    /** Application schema created and owned by the tracked script. */
    public static final String GU_SCHEMA = "gu";

    /** Tracked script that creates the schema; only used in operator-facing messages. */
    public static final String SCRIPT_RELATIVE_PATH = "DEVELOPPEMENT/database/gu.sql";

    /**
     * The ten relations of the {@code gu} schema together with the columns the API depends on. The
     * order mirrors the script so that the first reported failure is the most explanatory one.
     */
    public static final List<GuRelation> GU_RELATIONS = List.of(
        relation("type_utilisateur", "code", "tu_name"),
        relation("basic_rights", "code", "br_name"),
        relation("type_utilisateur_basic_right", "type_utilisateur_id", "basic_right_id", "\"dateCreation\""),
        relation("utilisateurs", "type_utilisateur_id", "nom", "prenom", "email", "login", "statut", "\"dateCreation\""),
        relation("password_history", "utilisateur_id", "\"password\"", "\"current\"", "date_insertion", "date_changement"),
        relation("client_particulier", "utilisateur_id"),
        relation("client_entreprise", "utilisateur_id", "raison_sociale", "niu", "rccm"),
        relation("registration_confirmation", "utilisateur_id", "token_hash", "expires_at", "confirmed_at", "date_creation"),
        relation("password_reset", "utilisateur_id", "token_hash", "expires_at", "used_at", "date_creation"),
        relation("sessions_utilisateur", "utilisateur_id", "session_hash", "browser_label", "remember_me",
            "date_creation", "last_seen_at", "expires_at", "invalidated_at")
    );

    private static GuRelation relation(String name, String... columns) {
        return new GuRelation(name, List.of(columns));
    }

    private GuSchemaCatalog() {
    }

    /**
     * One {@code gu} relation and the columns the API reads from it.
     *
     * @param name the relation name inside the {@code gu} schema
     * @param columns the expected column names, quoted exactly as PostgreSQL stores them
     */
    public record GuRelation(String name, List<String> columns) {

        public GuRelation {
            columns = List.copyOf(columns);
        }

        /** Qualified name such as {@code gu.utilisateurs}; safe to log and to return to operators. */
        public String qualifiedName() {
            return GU_SCHEMA + "." + name;
        }

        /** {@code LIMIT 0} probe that validates the relation without reading any row. */
        public String relationProbe() {
            return "SELECT 1 FROM " + qualifiedName() + " LIMIT 0";
        }

        /** {@code LIMIT 0} probe that validates one expected column without reading any row. */
        public String columnProbe(String column) {
            return "SELECT " + column + " FROM " + qualifiedName() + " LIMIT 0";
        }
    }
}
