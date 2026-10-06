package cm.odigital.serviceconnectmarket.auth.domain;

/** Generates the temporary password an administrator hands over when resetting an account. */
public interface TemporaryPasswordGenerator {

    String generate();
}
