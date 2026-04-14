package com.webdev.greenify.garden.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import com.webdev.greenify.garden.enumeration.PlantStage;
import com.webdev.greenify.garden.enumeration.PlantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantProgressResponse {

    private String seedId;
    private String seedName;
    private Integer progressDays;
    private Integer daysToMature;
    private PlantCycleType cycleType;
    private PlantStage currentStage;
    private PlantStatus status;
    private String currentStageImageUrl;
    private LocalDateTime startedAt;
    private Double percentComplete;
}
