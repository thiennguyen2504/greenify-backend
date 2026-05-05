package com.webdev.greenify.co2e.gemini.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiAnalysisRequest {

    private String imageUrl;
    private String caption;
    private String actionTypeName;
}
