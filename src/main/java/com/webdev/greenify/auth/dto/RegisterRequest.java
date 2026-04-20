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
    @NotBlank(message = "Verification token là bắt buộc")
    private String verificationToken;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;

    @NotBlank(message = "Mật khẩu xác nhận là bắt buộc")
    private String confirmPassword;
}
