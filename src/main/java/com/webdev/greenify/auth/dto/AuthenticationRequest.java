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
public class AuthenticationRequest {

    @NotBlank(message = "Tài khoản đăng nhập là bắt buộc")
    private String identifier;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;
}
