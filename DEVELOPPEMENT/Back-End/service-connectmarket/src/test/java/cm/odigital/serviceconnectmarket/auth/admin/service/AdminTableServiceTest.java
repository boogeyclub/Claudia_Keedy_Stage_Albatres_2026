package cm.odigital.serviceconnectmarket.auth.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import cm.odigital.serviceconnectmarket.auth.admin.domain.AdminTable;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminTableRepository;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminUserRecord;
import cm.odigital.serviceconnectmarket.auth.admin.persistence.AdminUserTypeRecord;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;

@ExtendWith(MockitoExtension.class)
class AdminTableServiceTest {

    @Mock
    private AdminTableRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminTableService service;

    @BeforeEach
    void setUp() {
        service = new AdminTableService(
            repository,
            passwordEncoder,
            Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void refusesDirectCreationOfSensitiveSessionRows() {
        AuthException exception = assertThrows(
            AuthException.class,
            () -> service.create(AdminTable.SESSIONS_UTILISATEUR, Map.of(), 1L)
        );

        assertEquals("ADMIN_TABLE_READ_ONLY", exception.getCode());
        verify(repository, never()).findRows(any());
    }

    @Test
    void refusesPasswordHistoryRemoval() {
        AuthException exception = assertThrows(
            AuthException.class,
            () -> service.remove(AdminTable.PASSWORD_HISTORY, "19", 1L)
        );

        assertEquals("ADMIN_TABLE_IMMUTABLE", exception.getCode());
        verify(repository, never()).deletePasswordHistory(anyLong());
    }

    @Test
    void rejectsASecretLikeFieldOutsideTheTableAllowList() {
        AuthException exception = assertThrows(
            AuthException.class,
            () -> service.create(AdminTable.TYPE_UTILISATEUR, Map.of(
                "code", "REVIEWER",
                "name", "Reviewer",
                "sessionHash", "must-not-be-accepted"
            ), 1L)
        );

        assertEquals("ADMIN_MUTATION_FIELD_FORBIDDEN", exception.getCode());
        verify(repository, never()).insertUserType("REVIEWER", "Reviewer");
    }

    @Test
    void createsAControlledUserByHashingTheSubmittedPasswordBeforePersistence() {
        AdminUserTypeRecord vendeurType = new AdminUserTypeRecord(3L, "VENDEUR", "Vendeur");
        when(repository.findUserType(3L)).thenReturn(Optional.of(vendeurType));
        when(repository.utilisateurHasAppConnection(3L)).thenReturn(true);
        when(repository.identityExists("amina@example.com", "amina-cocoa", null)).thenReturn(false);
        when(repository.insertUtilisateur(
            eq(3L),
            eq("Ngono"),
            eq("Amina"),
            eq("amina@example.com"),
            eq("amina-cocoa"),
            eq("ACTIF"),
            any(Instant.class)
        )).thenReturn(42L);
        when(passwordEncoder.encode("secure-passphrase")).thenReturn("bcrypt-hash-only");

        service.create(AdminTable.UTILISATEURS, Map.of(
            "typeUtilisateurId", 3,
            "nom", "Ngono",
            "prenom", "Amina",
            "email", "amina@example.com",
            "login", "amina-cocoa",
            "statut", "ACTIF",
            "password", "secure-passphrase"
        ), 1L);

        verify(passwordEncoder).encode("secure-passphrase");
        verify(repository).insertPasswordHash(
            42L,
            "bcrypt-hash-only",
            Instant.parse("2026-09-29T12:00:00Z")
        );
    }

    @Test
    void requiresTheRegistrationWorkflowWhenAnAdministratorTriesToCreateAClientAccount() {
        when(repository.findUserType(3L)).thenReturn(Optional.of(new AdminUserTypeRecord(3L, "CLIENT", "Client")));
        when(repository.utilisateurHasAppConnection(3L)).thenReturn(true);

        AuthException exception = assertThrows(
            AuthException.class,
            () -> service.create(AdminTable.UTILISATEURS, Map.of("typeUtilisateurId", 3), 1L)
        );

        assertEquals("ADMIN_CLIENT_CREATION_REQUIRES_REGISTRATION", exception.getCode());
        verify(repository, never()).insertUtilisateur(
            anyLong(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void updatesEnterpriseClientDetailsUsingTheControlledProfileTable() {
        when(repository.enterpriseIdentifiersExist("M012345678901A", "RC/YAO/2026/B/123", 42L)).thenReturn(false);
        when(repository.updateClientEntreprise(
            42L,
            "Cacao Source Cameroun SARL",
            "M012345678901A",
            "RC/YAO/2026/B/123"
        )).thenReturn(true);

        service.update(AdminTable.CLIENT_ENTREPRISE, "42", Map.of(
            "raisonSociale", "Cacao Source Cameroun SARL",
            "niu", "m012345678901a",
            "rccm", "rc/yao/2026/b/123"
        ), 1L);

        verify(repository).updateClientEntreprise(
            42L,
            "Cacao Source Cameroun SARL",
            "M012345678901A",
            "RC/YAO/2026/B/123"
        );
    }

    @Test
    void preventsDeletionOfTheLastActiveAdministratorUnderALock() {
        when(repository.findUtilisateur(2L)).thenReturn(Optional.of(new AdminUserRecord(
            2L, 1L, "ADMINISTRATEUR", "Admin", "Only", "only@example.com", "only-admin", "ACTIF"
        )));
        when(repository.countActiveAdministrators()).thenReturn(1L);

        AuthException exception = assertThrows(
            AuthException.class,
            () -> service.remove(AdminTable.UTILISATEURS, "2", 1L)
        );

        assertEquals("ADMIN_LAST_ADMINISTRATOR_PROTECTED", exception.getCode());
        verify(repository).lockActiveAdministrators();
        verify(repository, never()).deleteUtilisateur(2L);
    }

    @Test
    void preventsRemovalOfTheRequiredAppConnectionRightFromABuiltInRole() {
        when(repository.findUserType(3L)).thenReturn(Optional.of(new AdminUserTypeRecord(3L, "CLIENT", "Client")));
        when(repository.findBasicRightCode(1L)).thenReturn(Optional.of("APP-CONN"));

        AuthException exception = assertThrows(
            AuthException.class,
            () -> service.remove(AdminTable.TYPE_UTILISATEUR_BASIC_RIGHT, "3:1", 1L)
        );

        assertEquals("ADMIN_RIGHT_ASSIGNMENT_PROTECTED", exception.getCode());
        verify(repository, never()).deleteTypeUserBasicRight(3L, 1L);
    }
}
