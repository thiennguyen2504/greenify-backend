package com.webdev.greenify.garden.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.garden.enumeration.PlantStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantDailyLogResponse {

    private LocalDate logDate;
    private PlantStage stage;
    private Boolean isActiveDay;
    private String imageUrl;
}
