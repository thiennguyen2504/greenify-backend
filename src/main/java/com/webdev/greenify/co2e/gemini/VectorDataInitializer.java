package com.webdev.greenify.co2e.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webdev.greenify.co2e.entity.LocalKnowledge;
import com.webdev.greenify.co2e.repository.LocalKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VectorDataInitializer implements CommandLineRunner {

    private final LocalKnowledgeRepository repository;
    private final GeminiEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        try {
            if (repository.count() > 0) {
                log.info("Vector database already contains data. Skipping initialization.");
                return;
            }

            log.info("Vector database is empty. Starting data seeding...");
            ClassPathResource resource = new ClassPathResource("co2e-local-knowledge.json");
            if (!resource.exists()) {
                log.warn("co2e-local-knowledge.json not found in resources!");
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                Map<String, Map<String, String>> data = objectMapper.readValue(
                        inputStream,
                        new TypeReference<Map<String, Map<String, String>>>() {}
                );

                int requestCount = 0;
                int totalSeeded = 0;

                // Process Slang Map
                if (data.containsKey("slangMap") && data.get("slangMap") != null) {
                    Map<String, String> slangMap = data.get("slangMap");
                    for (Map.Entry<String, String> entry : slangMap.entrySet()) {
                        if (processItem(entry.getKey(), entry.getValue(), "slang")) {
                            totalSeeded++;
                        }
                        
                        requestCount++;
                        if (requestCount % 100 == 0) {
                            log.info("Reached 100 requests. Sleeping 61s to respect Gemini Rate Limit...");
                            Thread.sleep(61000);
                        } else {
                            Thread.sleep(100); // Small delay between normal requests
                        }
                    }
                }

                // Process Product Catalog
                if (data.containsKey("productCatalog") && data.get("productCatalog") != null) {
                    Map<String, String> productCatalog = data.get("productCatalog");
                    for (Map.Entry<String, String> entry : productCatalog.entrySet()) {
                        if (processItem(entry.getKey(), entry.getValue(), "product")) {
                            totalSeeded++;
                        }

                        requestCount++;
                        if (requestCount % 100 == 0) {
                            log.info("Reached 100 requests. Sleeping 61s to respect Gemini Rate Limit...");
                            Thread.sleep(61000);
                        } else {
                            Thread.sleep(100); // Small delay between normal requests
                        }
                    }
                }

                log.info("Successfully seeded {} records into vector database.", totalSeeded);
            }
        } catch (Exception e) {
            log.error("Error during vector data initialization", e);
        }
    }

    private boolean processItem(String name, String description, String type) {
        try {
            String textToEmbed = name + ": " + description;
            float[] embedding = embeddingService.getEmbedding(textToEmbed);
            if (embedding != null) {
                LocalKnowledge entity = LocalKnowledge.builder()
                        .type(type)
                        .name(name)
                        .description(description)
                        .embedding(embedding)
                        .build();
                repository.save(entity);
                return true;
            } else {
                log.warn("Failed to get embedding for {}: {}", type, name);
            }
        } catch (Exception e) {
            log.error("Failed to process {} item: {}", type, name, e);
        }
        return false;
    }
}
