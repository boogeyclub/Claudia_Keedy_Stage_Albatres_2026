package cm.odigital.serviceconnectmarket.auth.domain;

public interface ConfirmationTokenGenerator {

    String generate();

    String hash(String rawToken);
}
