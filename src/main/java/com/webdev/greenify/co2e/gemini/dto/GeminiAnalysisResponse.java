package com.webdev.greenify.co2e.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiAnalysisResponse {

    @JsonProperty("material_code")
    private String materialCode;

    private Double confidence;

    @JsonProperty("estimated_weight_kg")
    private Double estimatedWeightKg;

    private Integer quantity;

    private String reasoning;

    private boolean available;

    private String failureReason;

    public static GeminiAnalysisResponse unavailable(String failureReason) {
        return GeminiAnalysisResponse.builder()
                .materialCode("UNKNOWN")
                .confidence(0.0)
                .available(false)
                .failureReason(failureReason)
                .build();
    }

    public boolean isAvailable() {
        return available;
    }
}
