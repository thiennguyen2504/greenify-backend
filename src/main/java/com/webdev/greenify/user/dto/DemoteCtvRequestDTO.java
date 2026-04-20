package com.webdev.greenify.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoteCtvRequestDTO {

    @NotBlank(message = "Lý do hạ cấp là bắt buộc")
    private String reason;
}