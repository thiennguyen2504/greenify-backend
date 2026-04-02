package com.webdev.greenify.greenaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGreenActionPostRequest {

    @NotBlank(message = "Action type ID is required")
    private String actionTypeId;

    private String caption;

    @NotBlank(message = "Media URL is required")
    private String mediaUrl;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @NotNull(message = "Action date is required")
    private LocalDate actionDate;
}
