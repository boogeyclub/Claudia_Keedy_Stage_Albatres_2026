package cm.odigital.serviceconnectmarket.market.domain;

/** Life cycle of a visit proposal: PROPOSE, then ACCEPTE or REFUSE, then CONFIRME once both point validations are in. */
public enum AppointmentStatus {
    PROPOSE("PROPOSE"),
    ACCEPTE("ACCEPTE"),
    REFUSE("REFUSE"),
    ANNULE("ANNULE"),
    /** Both participants approved the GPS pin: the visit is set and can no longer be moved here. */
    CONFIRME("CONFIRME");
    private final String databaseValue;

    AppointmentStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
