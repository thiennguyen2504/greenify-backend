package com.webdev.greenify.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NGOProfileRejectRequestDTO {
    @NotBlank(message = "Lý do từ chối là bắt buộc")
    private String reason;
}
