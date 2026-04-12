package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransactionEntity, String> {

    /**
     * Calculate accumulated points (total positive points earned) for a user.
     * This includes all points ever earned, regardless of expiration.
     */
    @Query("""
            SELECT COALESCE(SUM(pt.points), 0)
            FROM PointTransactionEntity pt
            WHERE pt.user.id = :userId
            AND pt.points > 0
            """)
    BigDecimal sumAccumulatedPointsByUserId(@Param("userId") String userId);

    /**
     * Calculate available points (current usable points) for a user.
     * This is total points minus expired points (includes negative point transactions).
     */
    @Query("""
            SELECT COALESCE(SUM(pt.points), 0)
            FROM PointTransactionEntity pt
            WHERE pt.user.id = :userId
            """)
    BigDecimal sumAvailablePointsByUserId(@Param("userId") String userId);

    /**
     * Get paginated point transaction history for a user.
     * Ordered by createdAt DESC (most recent first).
     */
    @Query("""
            SELECT pt FROM PointTransactionEntity pt
            WHERE pt.user.id = :userId
            ORDER BY pt.createdAt DESC
            """)
    Page<PointTransactionEntity> findByUserIdOrderByCreatedAtDesc(
            @Param("userId") String userId,
            Pageable pageable);

    /**
     * Count total transactions for a user using JPQL COUNT.
     */
    @Query("""
            SELECT COUNT(pt)
            FROM PointTransactionEntity pt
            WHERE pt.user.id = :userId
            """)
    long countByUserId(@Param("userId") String userId);

    /**
     * Find expired point transactions that haven't been processed yet.
     * Used by scheduler to create deduction records.
     */
    @Query("""
            SELECT pt FROM PointTransactionEntity pt
            WHERE pt.expiresAt IS NOT NULL
            AND pt.expiresAt <= :currentTime
            AND pt.points > 0
            AND NOT EXISTS (
                SELECT 1 FROM PointTransactionEntity deduction
                WHERE deduction.expiredTransactionId = pt.id
            )
            """)
    List<PointTransactionEntity> findExpiredPointsNotProcessed(@Param("currentTime") LocalDateTime currentTime);
}
