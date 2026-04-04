package com.webdev.greenify.greenaction.dto.request;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import jakarta.validation.Valid;
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

    @NotNull(message = "Media image is required")
    @Valid
    private ImageRequestDTO media;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDate actionDate;
}
