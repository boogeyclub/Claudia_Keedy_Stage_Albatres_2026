package cm.odigital.serviceconnectmarket.schema;

import java.util.List;

/**
 * Result of one schema verification pass.
 *
 * @param status the overall verdict
 * @param missingElements unusable relations and columns, qualified as {@code gu.table} or
 *     {@code gu.table.column}; empty when the schema is complete
 */
public record SchemaVerificationOutcome(Status status, List<String> missingElements) {

    /** Possible verdicts of a schema verification pass. */
    public enum Status {
        /** Every relation and every required column is readable. */
        COMPLETE,
        /** PostgreSQL answered but the schema is missing relations or columns. */
        SCHEMA_INCOMPLETE,
        /** PostgreSQL could not be reached at all. */
        DATABASE_UNAVAILABLE
    }

    public SchemaVerificationOutcome {
        missingElements = List.copyOf(missingElements);
    }

    public static SchemaVerificationOutcome complete() {
        return new SchemaVerificationOutcome(Status.COMPLETE, List.of());
    }

    public static SchemaVerificationOutcome incomplete(List<String> missingElements) {
        return new SchemaVerificationOutcome(Status.SCHEMA_INCOMPLETE, missingElements);
    }

    public static SchemaVerificationOutcome databaseUnavailable() {
        return new SchemaVerificationOutcome(Status.DATABASE_UNAVAILABLE, List.of());
    }

    public boolean verified() {
        return status == Status.COMPLETE;
    }

    /** Single-line summary used by the startup log and the database health endpoint. */
    public String missingElementsSummary() {
        return missingElements.isEmpty() ? "[none]" : String.join(", ", missingElements);
    }
}
