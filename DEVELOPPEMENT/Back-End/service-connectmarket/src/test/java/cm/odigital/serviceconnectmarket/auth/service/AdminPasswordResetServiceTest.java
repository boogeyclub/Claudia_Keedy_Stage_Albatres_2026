package cm.odigital.serviceconnectmarket.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
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

import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminTableRepository;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminUtilisateurRecord;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.RegistrationLanguage;
import cm.odigital.serviceconnectmarket.auth.domain.TemporaryPasswordGenerator;
import cm.odigital.serviceconnectmarket.auth.messaging.CredentialsMessage;
import cm.odigital.serviceconnectmarket.auth.messaging.RegistrationMessagingService;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionRepository;

@ExtendWith(MockitoExtension.class)
class AdminPasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T09:00:00Z");
    private static final AdminUtilisateurRecord VENDEUR =
        new AdminUtilisateurRecord(42L, "amina@example.com", "Amina", "amina-cocoa", "ACTIF", "VENDEUR");

    @Mock
    private AdminTableRepository adminTableRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Mock
    private RegistrationMessagingService messagingService;

    private AdminPasswordResetService adminPasswordResetService;

    @BeforeEach
    void setUp() {
        adminPasswordResetService = new AdminPasswordResetService(
            adminTableRepository,
            authRepository,
            userSessionRepository,
            passwordEncoder,
            temporaryPasswordGenerator,
            messagingService,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void sendsGeneratedCredentialsAndDisconnectsEverySession() {
        when(adminTableRepository.findUtilisateurForPasswordReset(42L)).thenReturn(Optional.of(VENDEUR));
        when(temporaryPasswordGenerator.generate()).thenReturn("Temp-Pass24x");
        when(passwordEncoder.encode("Temp-Pass24x")).thenReturn("temporary-bcrypt-hash");

        AdminUtilisateurRecord reset = adminPasswordResetService.resetPassword(42L, 1L);

        assertEquals("amina-cocoa", reset.login());
        assertEquals("amina@example.com", reset.email());

        ArgumentCaptor<CredentialsMessage> message = ArgumentCaptor.forClass(CredentialsMessage.class);
        verify(messagingService).sendCredentials(message.capture());
        assertEquals("amina@example.com", message.getValue().recipientEmail());
        assertEquals("Amina", message.getValue().recipientFirstName());
        assertEquals("amina-cocoa", message.getValue().login());
        assertEquals("Temp-Pass24x", message.getValue().password());
        assertEquals(RegistrationLanguage.FR, message.getValue().language());

        verify(authRepository).insertPasswordHash(42L, "temporary-bcrypt-hash", NOW);
        verify(authRepository).markPasswordResetUsed(42L, NOW);
        verify(userSessionRepository).revokeAllForUtilisateur(42L, NOW);
    }

    @Test
    void refusesAnAccountThatCannotSignInAndSendsNothing() {
        when(adminTableRepository.findUtilisateurForPasswordReset(42L)).thenReturn(Optional.of(
            new AdminUtilisateurRecord(42L, "amina@example.com", "Amina", "amina-cocoa", "EN_ATTENTE_CONFIRMATION", "VENDEUR")
        ));

        AuthException exception = assertThrows(
            AuthException.class,
            () -> adminPasswordResetService.resetPassword(42L, 1L)
        );

        assertEquals("ADMIN_PASSWORD_RESET_ACCOUNT_NOT_ACTIVE", exception.getCode());
        verify(messagingService, never()).sendCredentials(any());
        verify(authRepository, never()).insertPasswordHash(anyLong(), any(), any());
    }

    @Test
    void refusesAnUnknownAccount() {
        when(adminTableRepository.findUtilisateurForPasswordReset(42L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(
            AuthException.class,
            () -> adminPasswordResetService.resetPassword(42L, 1L)
        );

        assertEquals("ADMIN_PASSWORD_RESET_USER_NOT_FOUND", exception.getCode());
        assertNotNull(exception.getMessage());
        verify(messagingService, never()).sendCredentials(any());
    }
}
