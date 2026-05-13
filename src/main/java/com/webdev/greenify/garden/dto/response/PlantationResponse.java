package com.webdev.greenify.garden.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.garden.enumeration.PlantationBuilding;
import com.webdev.greenify.user.dto.UserProfileResponseDTO;
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
public class PlantationResponse {

    private String id;
    private String seedId;
    private String seedName;
    private String seedStage4ImageUrl;
    private UserProfileResponseDTO user;
    private String slotId;
    private PlantationBuilding building;
    private LocalDateTime createdAt;
    private LocalDateTime wiltedAt;
}
