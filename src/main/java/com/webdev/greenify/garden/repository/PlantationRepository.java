package com.webdev.greenify.garden.repository;

import com.webdev.greenify.garden.entity.PlantationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlantationRepository extends JpaRepository<PlantationEntity, String> {

    Optional<PlantationEntity> findByIdAndIsDeletedFalse(String id);

    List<PlantationEntity> findAllByIsDeletedFalseAndWiltedAtAfter(LocalDateTime now);
}
