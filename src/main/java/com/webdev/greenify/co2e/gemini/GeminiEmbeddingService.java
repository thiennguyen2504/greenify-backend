package com.webdev.greenify.co2e.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webdev.greenify.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiEmbeddingService {

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public float[] getEmbedding(String text) {
        if (!geminiProperties.isEnabled()) {
            return null;
        }
        
        int maxRetries = 3;
        int retryCount = 0;

        String modelTag = "gemini-embedding-2";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" 
                     + modelTag 
                     + ":embedContent?key=" 
                     + geminiProperties.getApiKey();

        while (retryCount < maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                Map<String, Object> part = new HashMap<>();
                part.put("text", text);
                
                Map<String, Object> content = new HashMap<>();
                content.put("parts", new Object[]{part});
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", modelTag);
                requestBody.put("content", content);
                
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                
                String responseStr = restTemplate.postForObject(url, requestEntity, String.class);
                JsonNode rootNode = objectMapper.readTree(responseStr);
                
                if (rootNode.has("embedding") && rootNode.get("embedding").has("values")) {
                    JsonNode valuesNode = rootNode.get("embedding").get("values");
                    float[] embedding = new float[valuesNode.size()];
                    for (int i = 0; i < valuesNode.size(); i++) {
                        embedding[i] = (float) valuesNode.get(i).asDouble();
                    }
                    return embedding;
                } else {
                    log.error("Failed to extract embedding from Gemini API response: {}", responseStr);
                    return null;
                }
            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
                retryCount++;
                log.warn("Gemini API 503 error. Retry attempt {}/{}...", retryCount, maxRetries);
                try { Thread.sleep(1000 * retryCount); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                log.error("Error calling Gemini API for embedding: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }
}