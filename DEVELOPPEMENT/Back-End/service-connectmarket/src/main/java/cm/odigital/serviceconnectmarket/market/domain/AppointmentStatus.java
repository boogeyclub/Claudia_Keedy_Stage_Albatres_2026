package cm.odigital.serviceconnectmarket.market.domain;

/** Life cycle of a visit proposal. */
public enum AppointmentStatus {
    PROPOSE("PROPOSE"),
    ACCEPTE("ACCEPTE"),
    REFUSE("REFUSE"),
    ANNULE("ANNULE");
    private final String databaseValue;

    AppointmentStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
