package com.webdev.greenify.garden.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantTreeRequest {

    @NotBlank(message = "Archive ID la bat buoc")
    private String archiveId;

    @NotNull(message = "Toa do x la bat buoc")
    @DecimalMin(value = "0.0", message = "Toa do x phai nam trong khoang [0.0, 1.0]")
    @DecimalMax(value = "1.0", message = "Toa do x phai nam trong khoang [0.0, 1.0]")
    private Double xRatio;

    @NotNull(message = "Toa do y la bat buoc")
    @DecimalMin(value = "0.0", message = "Toa do y phai nam trong khoang [0.0, 1.0]")
    @DecimalMax(value = "1.0", message = "Toa do y phai nam trong khoang [0.0, 1.0]")
    private Double yRatio;
}
