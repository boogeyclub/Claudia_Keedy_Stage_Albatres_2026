package cm.odigital.serviceconnectmarket.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.session.UserSessionRepository;

@ExtendWith(MockitoExtension.class)
class AccountPasswordServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T09:00:00Z");

    @Mock
    private AuthRepository authRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountPasswordService accountPasswordService;

    @BeforeEach
    void setUp() {
        accountPasswordService = new AccountPasswordService(
            authRepository,
            userSessionRepository,
            passwordEncoder,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void replacesThePasswordAndDisconnectsEveryOtherBrowser() {
        when(authRepository.findCurrentPasswordHash(44L)).thenReturn(Optional.of("current-bcrypt-hash"));
        when(passwordEncoder.matches("old-passphrase", "current-bcrypt-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-passphrase", "current-bcrypt-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-passphrase")).thenReturn("new-bcrypt-hash");

        accountPasswordService.changePassword(44L, 9L, "old-passphrase", "new-passphrase");

        verify(authRepository).insertPasswordHash(44L, "new-bcrypt-hash", NOW);
        // The browser that changed the password stays connected; the others do not.
        verify(userSessionRepository).revokeAllForUtilisateurExcept(44L, 9L, NOW);
    }

    @Test
    void refusesAWrongCurrentPasswordWithoutTouchingTheStoredHash() {
        when(authRepository.findCurrentPasswordHash(44L)).thenReturn(Optional.of("current-bcrypt-hash"));
        when(passwordEncoder.matches("wrong-passphrase", "current-bcrypt-hash")).thenReturn(false);

        AuthException exception = assertThrows(
            AuthException.class,
            () -> accountPasswordService.changePassword(44L, 9L, "wrong-passphrase", "new-passphrase")
        );

        assertEquals("ACCOUNT_PASSWORD_CURRENT_INVALID", exception.getCode());
        verify(authRepository, never()).insertPasswordHash(anyLong(), any(), any());
        verify(userSessionRepository, never()).revokeAllForUtilisateurExcept(anyLong(), anyLong(), any());
    }

    @Test
    void refusesReusingTheCurrentPassword() {
        when(authRepository.findCurrentPasswordHash(44L)).thenReturn(Optional.of("current-bcrypt-hash"));
        when(passwordEncoder.matches("same-passphrase", "current-bcrypt-hash")).thenReturn(true);

        AuthException exception = assertThrows(
            AuthException.class,
            () -> accountPasswordService.changePassword(44L, 9L, "same-passphrase", "same-passphrase")
        );

        assertEquals("ACCOUNT_PASSWORD_UNCHANGED", exception.getCode());
        verify(authRepository, never()).insertPasswordHash(anyLong(), any(), any());
    }

    @Test
    void refusesATooShortPasswordBeforeReadingTheStoredHash() {
        AuthException exception = assertThrows(
            AuthException.class,
            () -> accountPasswordService.changePassword(44L, 9L, "old-passphrase", "short")
        );

        assertEquals("ACCOUNT_PASSWORD_TOO_SHORT", exception.getCode());
        verify(authRepository, never()).findCurrentPasswordHash(anyLong());
    }
}
