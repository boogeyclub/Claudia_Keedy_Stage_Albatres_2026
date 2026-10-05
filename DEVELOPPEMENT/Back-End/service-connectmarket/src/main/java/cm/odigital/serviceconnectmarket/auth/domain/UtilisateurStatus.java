package cm.odigital.serviceconnectmarket.auth.domain;

public enum UtilisateurStatus {
    PENDING_CONFIRMATION("EN_ATTENTE_CONFIRMATION"),
    ACTIVE("ACTIF");

    private final String databaseValue;

    UtilisateurStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
