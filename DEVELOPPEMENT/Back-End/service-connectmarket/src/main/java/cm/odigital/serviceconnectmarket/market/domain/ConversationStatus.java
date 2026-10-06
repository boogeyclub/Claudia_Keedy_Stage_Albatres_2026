package cm.odigital.serviceconnectmarket.market.domain;

/** Life cycle of a buyer/seller thread. */
public enum ConversationStatus {
    OUVERTE("OUVERTE"),
    EN_NEGOCIATION("EN_NEGOCIATION"),
    ACCORD("ACCORD"),
    CLOTUREE("CLOTUREE");
    private final String databaseValue;

    ConversationStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
