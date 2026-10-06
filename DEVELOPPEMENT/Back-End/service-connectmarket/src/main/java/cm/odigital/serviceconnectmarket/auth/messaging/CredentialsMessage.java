package cm.odigital.serviceconnectmarket.auth.messaging;

import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;

/**
 * Carries the new credentials an administrator generated for an account.
 *
 * It travels only between the password-reset service and the mail sender: the password is never
 * logged, never returned by an API and never stored in plaintext.
 */
public record CredentialsMessage(
    String recipientEmail,
    String recipientFirstName,
    String login,
    String password,
    RegistrationLanguage language
) {
}
