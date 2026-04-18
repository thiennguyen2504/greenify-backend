package com.webdev.greenify.greenaction.dto.response;

import com.webdev.greenify.greenaction.enumeration.EventFeasibilityConclusion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPredictionResponseDTO {
    private Double averageParticipants;
    private Double minRequirementRatio;
    private Double expectedRequirementRatio;
    private EventFeasibilityConclusion conclusion;
    private String message;
}
