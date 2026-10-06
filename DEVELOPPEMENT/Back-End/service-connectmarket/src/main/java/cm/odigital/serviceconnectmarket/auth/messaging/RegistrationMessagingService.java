package cm.odigital.serviceconnectmarket.auth.messaging;

public interface RegistrationMessagingService {

    void sendConfirmation(RegistrationConfirmationMessage message);

    void sendPasswordReset(PasswordResetMessage message);

    /**
     * Sends the credentials an administrator just generated. The plaintext password exists only in
     * this call and in the recipient mailbox.
     */
    void sendCredentials(CredentialsMessage message);
}
