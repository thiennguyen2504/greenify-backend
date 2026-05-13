package com.webdev.greenify.garden.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.webdev.greenify.garden.enumeration.PlantationBuilding;
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

    @NotBlank(message = "Slot ID là bắt buộc")
    private String slotId;

    @NotNull(message = "Khu vực là bắt buộc")
    private PlantationBuilding building;
}
