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
public class SuspendUserRequestDTO {

    @NotBlank(message = "Lý do khóa tài khoản là bắt buộc")
    private String reason;
}