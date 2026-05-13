package com.webdev.greenify.co2e.repository;

import com.webdev.greenify.co2e.entity.Co2eTransactionEntity;
import com.webdev.greenify.co2e.enumeration.Co2eTransactionStatus;
import com.webdev.greenify.co2e.enumeration.Co2eType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for CO2e transaction persistence and lookup.
 */
public interface Co2eTransactionRepository extends JpaRepository<Co2eTransactionEntity, String> {

    boolean existsByPostId(String postId);

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM co2e_transactions t
            WHERE t.user_id = :userId
              AND t.status = 'CREDITED'
              AND t.material_code IN ('REUSABLE_BOTTLE','REUSABLE_BAG','REUSABLE_BOX')
              AND CAST(t.created_at AS DATE) = :date
            """, nativeQuery = true)
    boolean existsReusableCreditedByUserOnDate(@Param("userId") String userId,
                                               @Param("date") LocalDate date);

    Page<Co2eTransactionEntity> findByUserIdAndStatusOrderByCreatedAtDesc(
            String userId,
            Co2eTransactionStatus status,
            Pageable pageable);

        long countByUserId(String userId);

    @Query("""
            SELECT COALESCE(SUM(t.co2eKg), 0)
            FROM Co2eTransactionEntity t
            WHERE t.status = :status
              AND t.co2eType = :type
              AND t.createdAt BETWEEN :start AND :end
            """)
    BigDecimal sumCo2eKgByStatusAndTypeBetween(
            @Param("status") Co2eTransactionStatus status,
            @Param("type") Co2eType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    Optional<Co2eTransactionEntity> findByPostId(String postId);
}
