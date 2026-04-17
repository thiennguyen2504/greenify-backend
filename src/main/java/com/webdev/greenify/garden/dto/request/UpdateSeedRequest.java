package com.webdev.greenify.garden.dto.request;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSeedRequest {

    @Size(max = 100, message = "Seed name must not exceed 100 characters")
    private String name;

    @Valid
    private ImageRequestDTO stage1Image;

    @Valid
    private ImageRequestDTO stage2Image;

    @Valid
    private ImageRequestDTO stage3Image;

    @Valid
    private ImageRequestDTO stage4Image;

    @Positive(message = "Days to mature must be greater than 0")
    private Integer daysToMature;

    @Positive(message = "Stage 2 from day must be greater than 0")
    private Integer stage2FromDay;

    @Positive(message = "Stage 3 from day must be greater than 0")
    private Integer stage3FromDay;

    @Positive(message = "Stage 4 from day must be greater than 0")
    private Integer stage4FromDay;

    private PlantCycleType cycleType;

    private String rewardVoucherTemplateId;

    private Boolean isActive;
}
