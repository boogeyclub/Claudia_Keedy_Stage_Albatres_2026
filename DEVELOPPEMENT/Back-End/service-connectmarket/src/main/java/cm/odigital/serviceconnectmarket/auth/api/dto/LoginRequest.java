package cm.odigital.serviceconnectmarket.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 255) String identity,
    @NotBlank @Size(min = 8, max = 72) String password,
    Boolean rememberMe
) {
}
