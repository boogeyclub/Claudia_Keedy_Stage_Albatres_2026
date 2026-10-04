package cm.odigital.serviceconnectmarket.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import cm.odigital.serviceconnectmarket.auth.domain.AuthException;
import cm.odigital.serviceconnectmarket.auth.domain.AuthenticatedUtilisateur;
import cm.odigital.serviceconnectmarket.auth.domain.UtilisateurStatus;
import cm.odigital.serviceconnectmarket.auth.persistence.AuthRepository;
import cm.odigital.serviceconnectmarket.auth.persistence.LoginCandidate;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RegistrationExpiryService registrationExpiryService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(authRepository, passwordEncoder, registrationExpiryService);
    }

    @Test
    void authenticatesAnActiveClientWithTheCurrentPassword() {
        LoginCandidate client = new LoginCandidate(
            7L,
            "buyer@example.com",
            "buyer-team",
            "Noah",
            "Buyer",
            "CLIENT",
            UtilisateurStatus.ACTIVE.databaseValue(),
            "bcrypt-password-hash"
        );
        when(authRepository.findLoginCandidate("buyer-team")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secure-passphrase", "bcrypt-password-hash")).thenReturn(true);

        AuthenticatedUtilisateur authenticated = authenticationService.authenticate("buyer-team", "secure-passphrase");

        verify(registrationExpiryService).removeExpiredRegistrations();
        assertEquals(7L, authenticated.id());
        assertEquals("CLIENT", authenticated.role());
    }

    @Test
    void authenticatesAnActiveAdministrateurWithTheAppConnectionRight() {
        LoginCandidate administrateur = new LoginCandidate(
            1L,
            "root@cacaomarket.local",
            "root",
            "Root",
            "System",
            "ADMINISTRATEUR",
            UtilisateurStatus.ACTIVE.databaseValue(),
            "bcrypt-password-hash"
        );
        when(authRepository.findLoginCandidate("root")).thenReturn(Optional.of(administrateur));
        when(passwordEncoder.matches("root1234", "bcrypt-password-hash")).thenReturn(true);

        AuthenticatedUtilisateur authenticated = authenticationService.authenticate("root", "root1234");

        assertEquals(1L, authenticated.id());
        assertEquals("ADMINISTRATEUR", authenticated.role());
    }

    @Test
    void deniesAPendingVendeurUntilTheEmailIsConfirmed() {
        LoginCandidate vendeur = new LoginCandidate(
            8L,
            "seller@example.com",
            "seller-team",
            "Amina",
            "Seller",
            "VENDEUR",
            UtilisateurStatus.PENDING_CONFIRMATION.databaseValue(),
            "bcrypt-password-hash"
        );
        when(authRepository.findLoginCandidate("seller-team")).thenReturn(Optional.of(vendeur));
        when(passwordEncoder.matches("secure-passphrase", "bcrypt-password-hash")).thenReturn(true);

        AuthException exception = assertThrows(
            AuthException.class,
            () -> authenticationService.authenticate("seller-team", "secure-passphrase")
        );

        assertEquals("REGISTRATION_PENDING_CONFIRMATION", exception.getCode());
    }
}
