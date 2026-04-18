package com.webdev.greenify.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NGOProfileRejectRequestDTO {
    @NotBlank(message = "Rejected reason is required")
    private String reason;
}
