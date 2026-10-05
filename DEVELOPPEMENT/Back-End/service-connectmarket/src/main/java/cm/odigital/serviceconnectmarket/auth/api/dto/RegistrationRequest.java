package cm.odigital.serviceconnectmarket.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
    @NotBlank @Pattern(regexp = "(?i)VENDEUR|CLIENT") String role,
    @Pattern(regexp = "(?i)PARTICULIER|ENTREPRISE") String clientProfileType,
    @Size(max = 150) String raisonSociale,
    @Size(max = 50) String niu,
    @Size(max = 50) String rccm,
    @NotBlank @Size(max = 100) String prenom,
    @NotBlank @Size(max = 100) String nom,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 3, max = 100) String login,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(min = 8, max = 72) String confirmPassword,
    @NotNull @AssertTrue Boolean acceptTerms,
    @Pattern(regexp = "(?i)en|fr") String language
) {

    @JsonIgnore
    @AssertTrue(message = "password confirmation must match the password")
    public boolean isPasswordConfirmationValid() {
        return password != null && password.equals(confirmPassword);
    }
}
