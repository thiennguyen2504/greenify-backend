package com.webdev.greenify.garden.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeedResponse {

    private String id;
    private String name;
    private String stage1ImageUrl;
    private String stage2ImageUrl;
    private String stage3ImageUrl;
    private String stage4ImageUrl;
    private Integer daysToMature;
    private Integer stage2FromDay;
    private Integer stage3FromDay;
    private Integer stage4FromDay;
    private PlantCycleType cycleType;
    private String rewardVoucherTemplateId;
    private String rewardVoucherName;
    private Boolean isActive;
}
