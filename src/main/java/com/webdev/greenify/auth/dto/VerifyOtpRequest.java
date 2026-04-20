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
public class VerifyOtpRequest {
    @NotBlank(message = "Định danh là bắt buộc")
    private String identifier;

    @NotBlank(message = "OTP là bắt buộc")
    private String otp;
}
