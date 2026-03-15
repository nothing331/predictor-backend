package api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DemoRegisterRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank @Email String email
) {}
