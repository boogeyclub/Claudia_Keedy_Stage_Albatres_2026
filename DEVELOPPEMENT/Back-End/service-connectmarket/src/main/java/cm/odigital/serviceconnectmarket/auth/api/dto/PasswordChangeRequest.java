package cm.odigital.serviceconnectmarket.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service password change submitted from the account settings page. */
public record PasswordChangeRequest(
    @NotBlank @Size(max = 512) String currentPassword,
    @NotBlank @Size(min = 8, max = 72) String newPassword,
    @NotBlank @Size(min = 8, max = 72) String confirmPassword
) {

    @JsonIgnore
    @AssertTrue(message = "password confirmation must match the new password")
    public boolean isPasswordConfirmationValid() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
