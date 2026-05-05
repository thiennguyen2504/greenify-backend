package com.webdev.greenify.co2e.gemini;

import com.webdev.greenify.co2e.entity.LocalKnowledge;
import com.webdev.greenify.co2e.repository.LocalKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalKnowledgeServiceImpl implements Co2eKnowledgeService {

    private final GeminiEmbeddingService embeddingService;
    private final LocalKnowledgeRepository repository;

    @Override
    public String getLocalContext(String caption) {
        if (caption == null || caption.isBlank()) {
            return "";
        }

        try {
            float[] embedding = embeddingService.getEmbedding(caption);
            if (embedding == null) {
                log.warn("Failed to get embedding for caption, returning empty context.");
                return "";
            }

            // Convert float[] to string format for Postgres vector casting: "[0.1, 0.2, ...]"
            String embeddingStr = Arrays.toString(embedding);

            List<LocalKnowledgeRepository.LocalKnowledgeProjection> similarKnowledge = repository.findTop5Similar(embeddingStr);
            if (similarKnowledge.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            StringBuilder slangsFound = new StringBuilder();
            StringBuilder productsFound = new StringBuilder();

            for (LocalKnowledgeRepository.LocalKnowledgeProjection kn : similarKnowledge) {
                if ("slang".equals(kn.getType())) {
                    slangsFound.append("- '").append(kn.getName()).append("': ").append(kn.getDescription()).append("\n");
                } else if ("product".equals(kn.getType())) {
                    productsFound.append("- '").append(kn.getName()).append("': ").append(kn.getDescription()).append("\n");
                }
            }

            if (slangsFound.length() > 0) {
                context.append("[Local Slang/Dialect]\n").append(slangsFound).append("\n");
            }
            if (productsFound.length() > 0) {
                context.append("[Local Product Weight Catalog]\n").append(productsFound).append("\n");
            }

            return context.toString();
        } catch (Exception e) {
            log.error("Error retrieving local context via vector search", e);
            return "";
        }
    }
}
