package cm.odigital.serviceconnectmarket.auth.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import cm.odigital.serviceconnectmarket.auth.config.RegistrationProperties;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;

@ExtendWith(MockitoExtension.class)
class SmtpRegistrationMessagingServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private RegistrationProperties registrationProperties;

    @BeforeEach
    void setUp() {
        registrationProperties = new RegistrationProperties();
    }

    @Test
    void sendsTheConfirmationThroughConfiguredGmailAndUsesTheMailboxAsTheDefaultSender() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        SmtpRegistrationMessagingService service = configuredService();

        service.sendConfirmation(new RegistrationConfirmationMessage(
            "buyer@example.com",
            "Noah",
            "https://api.example.test/api/auth/registration/confirm?token=single-use-token",
            Instant.parse("2026-09-22T15:00:00Z"),
            RegistrationLanguage.EN
        ));

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        assertEquals("cacaomarket.sender@gmail.com", email.getValue().getFrom());
        assertEquals("buyer@example.com", email.getValue().getTo()[0]);
        assertEquals("Confirm your CacaoMarketCM registration", email.getValue().getSubject());
        assertTrue(email.getValue().getText().contains("single-use-token"));
    }

    @Test
    void sendsThePasswordResetThroughConfiguredGmail() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        SmtpRegistrationMessagingService service = configuredService();

        service.sendPasswordReset(new PasswordResetMessage(
            "buyer@example.com",
            "Noah",
            "https://frontend.example.test/CacaoMarketCM/password-reset/confirm?token=single-use-token",
            Instant.parse("2026-09-23T13:00:00Z"),
            RegistrationLanguage.EN
        ));

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        assertEquals("buyer@example.com", email.getValue().getTo()[0]);
        assertEquals("Reset your CacaoMarketCM password", email.getValue().getSubject());
        assertTrue(email.getValue().getText().contains("single-use-token"));
        assertTrue(email.getValue().getText().contains("23 September 2026 at 13:00 UTC"));
    }

    @Test
    void failsSafelyBeforeSendingWhenGmailCredentialsAreMissing() {
        SmtpRegistrationMessagingService service = new SmtpRegistrationMessagingService(
            mailSenderProvider,
            registrationProperties,
            "smtp.gmail.com",
            "",
            ""
        );

        AuthException exception = assertThrows(AuthException.class, () -> service.sendConfirmation(new RegistrationConfirmationMessage(
            "buyer@example.com",
            "Noah",
            "https://api.example.test/api/auth/registration/confirm?token=single-use-token",
            Instant.parse("2026-09-22T15:00:00Z"),
            RegistrationLanguage.FR
        )));

        assertEquals("REGISTRATION_MAIL_DELIVERY_UNAVAILABLE", exception.getCode());
        verifyNoInteractions(mailSenderProvider, mailSender);
    }

    private SmtpRegistrationMessagingService configuredService() {
        return new SmtpRegistrationMessagingService(
            mailSenderProvider,
            registrationProperties,
            "smtp.gmail.com",
            "cacaomarket.sender@gmail.com",
            "google-app-password"
        );
    }
}
