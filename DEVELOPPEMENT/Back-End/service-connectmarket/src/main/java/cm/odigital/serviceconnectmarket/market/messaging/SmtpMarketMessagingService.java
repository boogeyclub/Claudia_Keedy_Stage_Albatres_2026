package cm.odigital.serviceconnectmarket.market.messaging;

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

import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.observability.AuditValue;

/**
 * SMTP delivery of the market notifications.
 *
 * <p>Delivery failures are logged and swallowed on purpose: the market action is already stored, and
 * an unusable mailbox must not make the messaging look broken. Recipients are always addressed in
 * French for now — the accounts of this deployment do not store a language preference yet.
 */
@Service
public class SmtpMarketMessagingService implements MarketMessagingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpMarketMessagingService.class);
    private static final RegistrationLanguage LANGUAGE = RegistrationLanguage.FR;
    private static final DateTimeFormatter EVENT_DATE = DateTimeFormatter
        .ofPattern("d MMMM uuuu 'à' HH:mm 'UTC'", Locale.FRENCH)
        .withZone(ZoneOffset.UTC);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;
    private final String senderAddress;

    public SmtpMarketMessagingService(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${spring.mail.host:}") String mailHost,
        @Value("${spring.mail.username:}") String mailUsername,
        @Value("${spring.mail.password:}") String mailPassword,
        @Value("${app.registration.mail-from:}") String configuredSender
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        // Same rule as the account emails: the configured mailbox is the fallback sender, because
        // Gmail only accepts the authenticated address (or a verified alias).
        this.senderAddress = StringUtils.hasText(configuredSender) ? configuredSender : mailUsername;
    }

    @Override
    public void send(MarketNotification notification) {
        if (!isConfigured()) {
            LOGGER.warn(
                "event=market.mail.skipped reason=SMTP_CONFIGURATION_MISSING type={} recipient={}",
                notification.type(),
                AuditValue.maskedEmail(notification.recipientEmail())
            );
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn(
                "event=market.mail.skipped reason=MAIL_SENDER_BEAN_UNAVAILABLE type={}",
                notification.type()
            );
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderAddress);
        email.setTo(notification.recipientEmail());
        email.setSubject(notification.type().subject(LANGUAGE));
        email.setText(body(notification));

        try {
            mailSender.send(email);
            LOGGER.info(
                "event=market.mail.sent type={} recipient={}",
                notification.type(),
                AuditValue.maskedEmail(notification.recipientEmail())
            );
        } catch (MailException exception) {
            LOGGER.warn(
                "event=market.mail.failed type={} exceptionType={}",
                notification.type(),
                exception.getClass().getName()
            );
        }
    }

    private String body(MarketNotification notification) {
        String greeting = notification.recipientFirstName() == null || notification.recipientFirstName().isBlank()
            ? "Bonjour,"
            : "Bonjour " + notification.recipientFirstName() + ",";

        StringBuilder body = new StringBuilder()
            .append(greeting).append("\n\n")
            .append(introduction(notification)).append("\n\n")
            .append("Annonce : ").append(notification.lotTitle()).append('\n');

        if (StringUtils.hasText(notification.detail())) {
            body.append(notification.detail()).append('\n');
        }

        body.append('\n')
            .append("Concerne : ").append(notification.counterpartName()).append('\n')
            .append("Le ").append(EVENT_DATE.format(notification.occurredAt())).append("\n\n");

        if (StringUtils.hasText(notification.conversationUrl())) {
            body.append("Reprendre l'échange : ").append(notification.conversationUrl()).append("\n\n");
        }

        return body
            .append("Ce message vous est envoyé par CacaoMarketCM parce qu'une action vient de démarrer sur l'une de vos conversations. Les échanges suivants restent dans votre messagerie.\n")
            .toString();
    }

    private String introduction(MarketNotification notification) {
        return switch (notification.type()) {
            case MESSAGE_INITIAL -> "Vous venez de recevoir le premier message d'un acheteur sur votre annonce.";
            case NEGOCIATION_PROPOSEE -> "Une proposition de prix et de volume vient d'être déposée dans votre conversation.";
            case RENDEZ_VOUS_PROPOSE -> "Une visite vient d'être proposée pour cette annonce.";
            case POSITION_DEMANDEE -> "L'acheteur vous demande de partager la position exacte du lot. Vous pouvez accepter, avec un point GPS, ou refuser.";
            case POSITION_A_VALIDER -> "Un point GPS vient d'être proposé pour la visite de ce lot : votre validation est nécessaire.";
            case POINT_REFUSE -> "Le point GPS proposé pour cette visite a été refusé. Une nouvelle proposition est attendue pour pouvoir acter le rendez-vous.";
            case VISITE_CONFIRMEE -> "Les deux parties ont validé le point GPS : la visite est confirmée.";
        };
    }

    private boolean isConfigured() {
        return StringUtils.hasText(mailHost)
            && StringUtils.hasText(mailUsername)
            && StringUtils.hasText(mailPassword);
    }
}
