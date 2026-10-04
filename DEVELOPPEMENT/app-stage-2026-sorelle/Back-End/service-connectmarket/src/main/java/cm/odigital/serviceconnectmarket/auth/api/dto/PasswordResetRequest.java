package cm.odigital.serviceconnectmarket.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @Pattern(regexp = "(?i)en|fr") String language
) {
}
