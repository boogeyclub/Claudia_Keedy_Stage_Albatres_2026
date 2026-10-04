package cm.odigital.serviceconnectmarket.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import cm.odigital.serviceconnectmarket.auth.config.PasswordResetProperties;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.ConfirmationTokenGenerator;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.auth.domain.UtilisateurStatus;
import cm.odigital.serviceconnectmarket.auth.messaging.PasswordResetMessage;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationMessagingService;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.persistence.PasswordResetCandidate;
import cm.odigital.serviceconnectmarket.auth.persistence.PasswordResetRecord;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    @Mock
    private AuthRepository authRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ConfirmationTokenGenerator tokenGenerator;

    @Mock
    private RegistrationMessagingService messagingService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setResetUrl("https://frontend.example.test/CacaoMarketCM/password-reset/confirm");
        properties.setTokenTtl(Duration.ofHours(1));
        passwordResetService = new PasswordResetService(
            authRepository,
            userSessionRepository,
            passwordEncoder,
            tokenGenerator,
            messagingService,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void sendsAOneHourResetLinkOnlyToAnActiveConfirmedAccount() {
        PasswordResetCandidate candidate = new PasswordResetCandidate(44L, "buyer@example.com", "Noah");
        when(authRepository.findActiveUtilisateurByEmail("buyer@example.com")).thenReturn(Optional.of(candidate));
        when(tokenGenerator.generate()).thenReturn("raw-reset-token");
        when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");

        passwordResetService.requestPasswordReset("BUYER@example.com", "fr");

        verify(authRepository).upsertPasswordReset(
            eq(44L),
            eq("hashed-reset-token"),
            eq(NOW.plus(Duration.ofHours(1))),
            eq(NOW)
        );
        ArgumentCaptor<PasswordResetMessage> message = ArgumentCaptor.forClass(PasswordResetMessage.class);
        verify(messagingService).sendPasswordReset(message.capture());
        assertEquals("buyer@example.com", message.getValue().recipientEmail());
        assertEquals(RegistrationLanguage.FR, message.getValue().language());
        assertEquals(NOW.plus(Duration.ofHours(1)), message.getValue().expiresAt());
        assertEquals(
            "https://frontend.example.test/CacaoMarketCM/password-reset/confirm?token=raw-reset-token",
            message.getValue().resetUrl()
        );
    }

    @Test
    void doesNotCreateATokenOrSendMailForAnUnknownOrUnconfirmedEmail() {
        when(authRepository.findActiveUtilisateurByEmail("pending@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("pending@example.com", "en");

        verify(authRepository, never()).upsertPasswordReset(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
        verify(messagingService, never()).sendPasswordReset(org.mockito.ArgumentMatchers.any());
        verify(tokenGenerator, never()).generate();
    }

    @Test
    void replacesThePasswordConsumesTheTokenAndDisconnectsEveryBrowser() {
        when(tokenGenerator.hash("valid-reset-token")).thenReturn("hashed-reset-token");
        when(authRepository.findPasswordResetByTokenHash("hashed-reset-token")).thenReturn(Optional.of(
            new PasswordResetRecord(44L, NOW.plus(Duration.ofHours(1)), null, UtilisateurStatus.ACTIVE.databaseValue())
        ));
        when(passwordEncoder.encode("new-secure-password")).thenReturn("new-bcrypt-password-hash");
        when(userSessionRepository.revokeAllForUtilisateur(44L, NOW)).thenReturn(2);

        passwordResetService.completePasswordReset("valid-reset-token", "new-secure-password");

        verify(authRepository).insertPasswordHash(44L, "new-bcrypt-password-hash", NOW);
        verify(authRepository).markPasswordResetUsed(44L, NOW);
        verify(userSessionRepository).revokeAllForUtilisateur(44L, NOW);
    }

    @Test
    void rejectsAnExpiredResetLinkWithoutChangingThePassword() {
        when(tokenGenerator.hash("expired-reset-token")).thenReturn("hashed-reset-token");
        when(authRepository.findPasswordResetByTokenHash("hashed-reset-token")).thenReturn(Optional.of(
            new PasswordResetRecord(44L, NOW.minusSeconds(1), null, UtilisateurStatus.ACTIVE.databaseValue())
        ));

        AuthException exception = assertThrows(
            AuthException.class,
            () -> passwordResetService.completePasswordReset("expired-reset-token", "new-secure-password")
        );

        assertEquals("PASSWORD_RESET_TOKEN_EXPIRED", exception.getCode());
        verify(authRepository, never()).insertPasswordHash(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        );
        verify(userSessionRepository, never()).revokeAllForUtilisateur(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any()
        );
    }
}
