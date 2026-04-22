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
public class ForgotPasswordSetPasswordRequest {

    @NotBlank(message = "Verification token là bắt buộc")
    private String verificationToken;

    @NotBlank(message = "Mật khẩu mới là bắt buộc")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu mới là bắt buộc")
    private String confirmNewPassword;
}
