package cm.odigital.serviceconnectmarket.auth.messaging;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cm.odigital.serviceconnectmarket.auth.config.RegistrationProperties;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.observability.AuditValue;

@Service
public class SmtpRegistrationMessagingService implements RegistrationMessagingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpRegistrationMessagingService.class);
    private static final DateTimeFormatter RESET_EXPIRY_ENGLISH = DateTimeFormatter
        .ofPattern("d MMMM uuuu 'at' HH:mm 'UTC'", Locale.ENGLISH)
        .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter RESET_EXPIRY_FRENCH = DateTimeFormatter
        .ofPattern("d MMMM uuuu 'à' HH:mm 'UTC'", Locale.FRENCH)
        .withZone(ZoneOffset.UTC);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RegistrationProperties registrationProperties;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;

    public SmtpRegistrationMessagingService(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        RegistrationProperties registrationProperties,
        @Value("${spring.mail.host:}") String mailHost,
        @Value("${spring.mail.username:}") String mailUsername,
        @Value("${spring.mail.password:}") String mailPassword
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.registrationProperties = registrationProperties;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
    }

    @Override
    public void sendConfirmation(RegistrationConfirmationMessage confirmation) {
        if (!isConfigured()) {
            LOGGER.warn(
                "event=registration.mail.dispatch.rejected reason=SMTP_CONFIGURATION_MISSING hostConfigured={} usernameConfigured={} passwordConfigured={}",
                StringUtils.hasText(mailHost),
                StringUtils.hasText(mailUsername),
                StringUtils.hasText(mailPassword)
            );
            throw AuthException.unavailable(
                "REGISTRATION_MAIL_DELIVERY_UNAVAILABLE",
                "Google SMTP email delivery is not configured."
            );
        }

        LOGGER.info(
            "event=registration.mail.smtp-send.started smtpHost={} recipient={}",
            mailHost,
            AuditValue.maskedEmail(confirmation.recipientEmail())
        );
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderAddress());
        email.setTo(confirmation.recipientEmail());
        email.setSubject(subjectFor(confirmation.language()));
        email.setText(bodyFor(confirmation));

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("event=registration.mail.dispatch.rejected reason=MAIL_SENDER_BEAN_UNAVAILABLE");
            throw AuthException.unavailable(
                "REGISTRATION_MAIL_DELIVERY_UNAVAILABLE",
                "Registration email delivery is not configured."
            );
        }

        try {
            mailSender.send(email);
            LOGGER.info("event=registration.mail.smtp-send.completed");
        } catch (MailException exception) {
            LOGGER.warn(
                "event=registration.mail.smtp-send.failed exceptionType={}",
                exception.getClass().getName()
            );
            throw AuthException.unavailable(
                "REGISTRATION_MAIL_DELIVERY_UNAVAILABLE",
                "The registration confirmation email could not be sent."
            );
        }
    }

    @Override
    public void sendPasswordReset(PasswordResetMessage reset) {
        if (!isConfigured()) {
            LOGGER.warn(
                "event=password-reset.mail.dispatch.rejected reason=SMTP_CONFIGURATION_MISSING hostConfigured={} usernameConfigured={} passwordConfigured={}",
                StringUtils.hasText(mailHost),
                StringUtils.hasText(mailUsername),
                StringUtils.hasText(mailPassword)
            );
            throw AuthException.unavailable(
                "PASSWORD_RESET_MAIL_DELIVERY_UNAVAILABLE",
                "Google SMTP email delivery is not configured."
            );
        }

        LOGGER.info(
            "event=password-reset.mail.smtp-send.started smtpHost={} recipient={}",
            mailHost,
            AuditValue.maskedEmail(reset.recipientEmail())
        );
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderAddress());
        email.setTo(reset.recipientEmail());
        email.setSubject(passwordResetSubjectFor(reset.language()));
        email.setText(passwordResetBodyFor(reset));

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("event=password-reset.mail.dispatch.rejected reason=MAIL_SENDER_BEAN_UNAVAILABLE");
            throw AuthException.unavailable(
                "PASSWORD_RESET_MAIL_DELIVERY_UNAVAILABLE",
                "Password reset email delivery is not configured."
            );
        }

        try {
            mailSender.send(email);
            LOGGER.info("event=password-reset.mail.smtp-send.completed");
        } catch (MailException exception) {
            LOGGER.warn(
                "event=password-reset.mail.smtp-send.failed exceptionType={}",
                exception.getClass().getName()
            );
            throw AuthException.unavailable(
                "PASSWORD_RESET_MAIL_DELIVERY_UNAVAILABLE",
                "The password reset email could not be sent."
            );
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(mailHost)
            && StringUtils.hasText(mailUsername)
            && StringUtils.hasText(mailPassword);
    }

    private String senderAddress() {
        return StringUtils.hasText(registrationProperties.getMailFrom())
            ? registrationProperties.getMailFrom()
            : mailUsername;
    }

    private String subjectFor(RegistrationLanguage language) {
        return language == RegistrationLanguage.FR
            ? "Confirmez votre inscription CacaoMarketCM"
            : "Confirm your CacaoMarketCM registration";
    }

    private String bodyFor(RegistrationConfirmationMessage confirmation) {
        if (confirmation.language() == RegistrationLanguage.FR) {
            return """
                Bonjour %s,

                Merci de vous inscrire sur CacaoMarketCM. Confirmez votre adresse e-mail en ouvrant le lien ci-dessous :

                %s

                Ce lien est personnel, à usage unique et valable pendant 3 heures. Après ce délai, votre inscription non confirmée sera refusée et supprimée. Vous devrez alors vous inscrire de nouveau.

                Si vous n'avez pas demandé cette inscription, vous pouvez ignorer cet e-mail.
                """.formatted(confirmation.recipientFirstName(), confirmation.confirmationUrl());
        }

        return """
            Hello %s,

            Thank you for registering with CacaoMarketCM. Confirm your email address by opening the link below:

            %s

            This personal, single-use link is valid for 3 hours. After that time, your unconfirmed registration is denied and removed, and you will need to register again.

            If you did not request this registration, you can safely ignore this email.
            """.formatted(confirmation.recipientFirstName(), confirmation.confirmationUrl());
    }

    private String passwordResetSubjectFor(RegistrationLanguage language) {
        return language == RegistrationLanguage.FR
            ? "Réinitialisez votre mot de passe CacaoMarketCM"
            : "Reset your CacaoMarketCM password";
    }

    private String passwordResetBodyFor(PasswordResetMessage reset) {
        if (reset.language() == RegistrationLanguage.FR) {
            return """
                Bonjour %s,

                Une demande de réinitialisation du mot de passe de votre compte CacaoMarketCM a été reçue. Pour choisir un nouveau mot de passe, ouvrez le lien ci-dessous :

                %s

                Ce lien personnel, à usage unique, expire le %s. Après avoir choisi un nouveau mot de passe, vous devrez vous reconnecter sur tous vos navigateurs.

                Si vous n'avez pas demandé cette réinitialisation, ignorez cet e-mail : votre mot de passe actuel reste inchangé.
                """.formatted(reset.recipientFirstName(), reset.resetUrl(), RESET_EXPIRY_FRENCH.format(reset.expiresAt()));
        }

        return """
            Hello %s,

            A password reset was requested for your CacaoMarketCM account. To choose a new password, open the link below:

            %s

            This personal, single-use link expires at %s. After choosing a new password, you will need to sign in again on every browser.

            If you did not request this reset, you can safely ignore this email: your current password remains unchanged.
            """.formatted(reset.recipientFirstName(), reset.resetUrl(), RESET_EXPIRY_ENGLISH.format(reset.expiresAt()));
    }
}
