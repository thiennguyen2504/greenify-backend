package com.webdev.greenify.garden.repository;

import com.webdev.greenify.garden.entity.PlantDailyLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface PlantDailyLogRepository extends JpaRepository<PlantDailyLogEntity, String> {

    boolean existsByUserIdAndLogDate(String userId, LocalDate logDate);
}
