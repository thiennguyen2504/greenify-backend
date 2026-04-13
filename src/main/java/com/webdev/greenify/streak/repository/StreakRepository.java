package com.webdev.greenify.streak.repository;

import com.webdev.greenify.streak.entity.StreakEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StreakRepository extends JpaRepository<StreakEntity, String> {

    @EntityGraph(attributePaths = {"user"})
    Optional<StreakEntity> findByUserId(String userId);
}
