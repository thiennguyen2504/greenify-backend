package com.webdev.greenify.point.repository;

import com.webdev.greenify.point.entity.PointWalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWalletEntity, String> {

    Optional<PointWalletEntity> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT pw
            FROM PointWalletEntity pw
            WHERE pw.userId = :userId
            """)
    Optional<PointWalletEntity> findByUserIdForUpdate(@Param("userId") String userId);

    @EntityGraph(attributePaths = {"user", "user.userProfile", "user.userProfile.avatar"})
    List<PointWalletEntity> findByUserIdIn(Collection<String> userIds);

    @Modifying
    @Query("""
            UPDATE PointWalletEntity pw
            SET pw.weeklyPoints = 0,
                pw.lastPointEarnedAt = null
            """)
    int resetWeeklyPointsForAllUsers();
}