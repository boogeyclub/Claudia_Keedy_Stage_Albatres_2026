package cm.odigital.serviceconnectmarket.market.domain;

/** Sale life cycle of a catalogue lot. */
public enum LotStatus {
    BROUILLON("BROUILLON"),
    PUBLIE("PUBLIE"),
    RESERVE("RESERVE"),
    VENDU("VENDU"),
    ARCHIVE("ARCHIVE");
    private final String databaseValue;

    LotStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
