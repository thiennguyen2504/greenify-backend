package com.webdev.greenify.garden.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeedRequest {

    @NotBlank(message = "Seed name is required")
    private String name;

    @NotBlank(message = "Stage 1 image URL is required")
    private String stage1ImageUrl;

    @NotBlank(message = "Stage 2 image URL is required")
    private String stage2ImageUrl;

    @NotBlank(message = "Stage 3 image URL is required")
    private String stage3ImageUrl;

    @NotBlank(message = "Stage 4 image URL is required")
    private String stage4ImageUrl;

    @NotNull(message = "Days to mature is required")
    @Positive(message = "Days to mature must be greater than 0")
    private Integer daysToMature;

    @NotNull(message = "Stage 2 from day is required")
    @Positive(message = "Stage 2 from day must be greater than 0")
    private Integer stage2FromDay;

    @NotNull(message = "Stage 3 from day is required")
    @Positive(message = "Stage 3 from day must be greater than 0")
    private Integer stage3FromDay;

    @NotNull(message = "Stage 4 from day is required")
    @Positive(message = "Stage 4 from day must be greater than 0")
    private Integer stage4FromDay;

    private String rewardVoucherTemplateId;
}
