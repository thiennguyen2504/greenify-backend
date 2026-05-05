package com.webdev.greenify.co2e.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webdev.greenify.co2e.gemini.dto.GeminiAnalysisRequest;
import com.webdev.greenify.co2e.gemini.dto.GeminiAnalysisResponse;
import com.webdev.greenify.config.GeminiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Gemini-backed implementation that calls the Generative Language API.
 */
@Service
@Slf4j
public class GeminiAnalysisServiceImpl implements GeminiAnalysisService {

    private static final long MIN_TIMEOUT_MILLIS = 500L;
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String DEFAULT_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final double DEFAULT_TEMPERATURE = 0.2;

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAnalysisServiceImpl(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
        this.restTemplate = buildRestTemplate(geminiProperties);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public GeminiAnalysisResponse analyzePost(String imageUrl, String caption, String actionTypeName) {
        log.info("GEMINI CONFIG: enabled={}, hasApiKey={}, model={}", 
                geminiProperties.isEnabled(), 
                hasText(geminiProperties.getApiKey()),
                geminiProperties.getModel());
        
        if (!geminiProperties.isEnabled()) {
            log.warn("Gemini disabled via GEMINI_ENABLED=false");
            return GeminiAnalysisResponse.unavailable("GEMINI_DISABLED");
        }
        if (!hasText(geminiProperties.getApiKey())) {
            log.warn("Gemini API key not set (GEMINI_API_KEY environment variable missing or empty)");
            return GeminiAnalysisResponse.unavailable("GEMINI_DISABLED");
        }

        GeminiAnalysisRequest request = GeminiAnalysisRequest.builder()
                .imageUrl(imageUrl)
                .caption(caption)
                .actionTypeName(actionTypeName)
                .build();

        String apiUrl = resolveApiUrl();
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", geminiProperties.getApiKey())
                .build(true)
                .toUri();

        try {
            Map<String, Object> payload = buildRequestPayload(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    uri,
                    new HttpEntity<>(payload, headers),
                    String.class);

            GeminiAnalysisResponse parsed = parseGeminiResponse(response.getBody());
            if (parsed == null) {
                return GeminiAnalysisResponse.unavailable("GEMINI_EMPTY_RESPONSE");
            }
            parsed.setAvailable(true);
            if (!hasText(parsed.getMaterialCode())) {
                parsed.setMaterialCode("UNKNOWN");
            }
            return parsed;
        } catch (RestClientResponseException ex) {
            log.warn("Gemini API error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            return GeminiAnalysisResponse.unavailable("GEMINI_UNAVAILABLE");
        } catch (RestClientException ex) {
            log.warn("Gemini API call failed: {}", ex.getMessage());
            return GeminiAnalysisResponse.unavailable("GEMINI_UNAVAILABLE");
        } catch (Exception ex) {
            log.warn("Gemini response parsing failed: {}", ex.getMessage());
            return GeminiAnalysisResponse.unavailable("GEMINI_UNAVAILABLE");
        }
    }

    private RestTemplate buildRestTemplate(GeminiProperties properties) {
        int timeoutMillis = (int) Math.max(properties.getTimeoutMillis(), MIN_TIMEOUT_MILLIS);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return new RestTemplate(requestFactory);
    }

    private String resolveApiUrl() {
        if (hasText(geminiProperties.getApiUrl())) {
            return geminiProperties.getApiUrl();
        }
        String model = hasText(geminiProperties.getModel()) ? geminiProperties.getModel() : DEFAULT_MODEL;
        return String.format(DEFAULT_API_URL_TEMPLATE, model);
    }

    private Map<String, Object> buildRequestPayload(GeminiAnalysisRequest request) {
        String prompt = buildPrompt(request);
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
                "temperature", DEFAULT_TEMPERATURE,
                "response_mime_type", "application/json");
        return Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig);
    }

    private String buildPrompt(GeminiAnalysisRequest request) {
        String imageUrl = hasText(request.getImageUrl()) ? request.getImageUrl() : "";
        String caption = hasText(request.getCaption()) ? request.getCaption() : "";
        String actionType = hasText(request.getActionTypeName()) ? request.getActionTypeName() : "";
        return "You are an assistant that classifies green actions for CO2e calculation. "
                + "Return only valid JSON with keys: material_code, confidence, estimated_weight_kg, quantity, reasoning. "
                + "Use material_code values from the predefined list (e.g., PET_PLASTIC, HDPE_PLASTIC, PP_PLASTIC, "
                + "MIXED_PLASTIC, PAPER, CARDBOARD, ALUMINUM_CAN, STEEL_CAN, METAL_MIXED, GLASS, MIXED_RECYCLABLES, "
                + "ORGANIC_COMPOST, CLEANUP_MIXED_WASTE, E_WASTE_SMALL, HAZARDOUS_WASTE, REUSABLE_BOTTLE, "
                + "REUSABLE_BAG, REUSABLE_BOX, DONATE_TEXTILE, TEXTILE_RECYCLE, TREE_PLANTING_PENDING, "
                + "TREE_PLANTING_VERIFIED, TREE_SURVIVED_6M, TREE_SURVIVED_1Y, TREE_EVENT_NGO, UNKNOWN). "
                + "If uncertain, use UNKNOWN and low confidence. "
                + "Input:\n"
                + "action_type: " + actionType + "\n"
                + "caption: " + caption + "\n"
                + "image_url: " + imageUrl;
    }

    private GeminiAnalysisResponse parseGeminiResponse(String body) throws Exception {
        if (!hasText(body)) {
            return null;
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode textNode = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");
        String rawText = textNode.isMissingNode() ? null : textNode.asText(null);
        String jsonPayload = extractJsonPayload(rawText);
        if (!hasText(jsonPayload)) {
            return null;
        }
        return objectMapper.readValue(jsonPayload, GeminiAnalysisResponse.class);
    }

    private String extractJsonPayload(String rawText) {
        if (!hasText(rawText)) {
            return null;
        }
        String cleaned = rawText.trim();
        if (cleaned.startsWith("```")) {
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return cleaned.substring(firstBrace, lastBrace + 1);
            }
        }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1);
        }
        return cleaned;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
