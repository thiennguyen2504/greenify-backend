package com.webdev.greenify.co2e.gemini;

import com.webdev.greenify.co2e.gemini.dto.GeminiAnalysisResponse;

/**
 * Service for sending post data to Gemini and parsing analysis results.
 */
public interface GeminiAnalysisService {

    GeminiAnalysisResponse analyzePost(String imageUrl, String caption, String actionTypeName);
}
