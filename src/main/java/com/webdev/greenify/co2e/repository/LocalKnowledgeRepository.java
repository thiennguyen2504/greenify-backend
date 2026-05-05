package com.webdev.greenify.co2e.repository;

import com.webdev.greenify.co2e.entity.LocalKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalKnowledgeRepository extends JpaRepository<LocalKnowledge, Long> {

    interface LocalKnowledgeProjection {
        Long getId();
        String getName();
        String getDescription();
        String getType();
    }

    @Query(value = "SELECT id, name, description, type FROM local_knowledge ORDER BY embedding <=> cast(?1 as vector) LIMIT 5", nativeQuery = true)
    List<LocalKnowledgeProjection> findTop5Similar(String embeddingStr);
}
