package com.webdev.greenify.garden.repository;

import com.webdev.greenify.garden.entity.PlantProgressEntity;
import com.webdev.greenify.garden.enumeration.PlantStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlantProgressRepository extends JpaRepository<PlantProgressEntity, String> {

    @EntityGraph(attributePaths = {"seed"})
    Optional<PlantProgressEntity> findByUserIdAndStatus(String userId, PlantStatus status);
}
