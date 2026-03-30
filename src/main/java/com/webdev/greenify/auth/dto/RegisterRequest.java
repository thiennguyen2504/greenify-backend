package com.webdev.greenify.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Verification token is required")
    private String verificationToken;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Confirm is required")
    private String confirmPassword;
}
