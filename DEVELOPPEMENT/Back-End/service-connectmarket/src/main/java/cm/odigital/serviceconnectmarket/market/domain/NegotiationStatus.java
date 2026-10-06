package cm.odigital.serviceconnectmarket.market.domain;

/** Life cycle of a price/volume proposal. */
public enum NegotiationStatus {
    PROPOSEE("PROPOSEE"),
    ACCEPTEE("ACCEPTEE"),
    REFUSEE("REFUSEE"),
    ANNULEE("ANNULEE"),
    EXPIREE("EXPIREE");
    private final String databaseValue;

    NegotiationStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
