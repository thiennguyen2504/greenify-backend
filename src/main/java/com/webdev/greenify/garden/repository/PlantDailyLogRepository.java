package com.webdev.greenify.garden.repository;

import com.webdev.greenify.garden.entity.PlantDailyLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlantDailyLogRepository extends JpaRepository<PlantDailyLogEntity, String>,
    JpaSpecificationExecutor<PlantDailyLogEntity> {

    boolean existsByUserIdAndLogDate(String userId, LocalDate logDate);

    List<PlantDailyLogEntity> findByUserIdOrderByLogDateAsc(String userId);

    Optional<PlantDailyLogEntity> findTopByPlantProgressIdAndIsActiveDayTrueOrderByLogDateDesc(String plantProgressId);
}
