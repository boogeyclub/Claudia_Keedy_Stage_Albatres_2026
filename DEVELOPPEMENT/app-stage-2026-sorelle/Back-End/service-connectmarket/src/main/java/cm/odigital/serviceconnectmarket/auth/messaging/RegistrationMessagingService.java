package cm.odigital.serviceconnectmarket.auth.messaging;

public interface RegistrationMessagingService {

    void sendConfirmation(RegistrationConfirmationMessage message);

    void sendPasswordReset(PasswordResetMessage message);
}
