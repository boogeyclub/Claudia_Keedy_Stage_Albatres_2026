package cm.odigital.serviceconnectmarket.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmationRequest(
    @NotBlank @Size(max = 512) String token,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(min = 8, max = 72) String confirmPassword
) {

    @JsonIgnore
    @AssertTrue(message = "password confirmation must match the password")
    public boolean isPasswordConfirmationValid() {
        return password != null && password.equals(confirmPassword);
    }
}
