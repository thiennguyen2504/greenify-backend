package com.webdev.greenify.co2e.repository;

import com.webdev.greenify.co2e.entity.LocalKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalKnowledgeRepository extends JpaRepository<LocalKnowledge, Long> {

    @Query(value = "SELECT * FROM local_knowledge ORDER BY embedding <=> cast(?1 as vector) LIMIT 5", nativeQuery = true)
    List<LocalKnowledge> findTop5Similar(String embeddingStr);
}
