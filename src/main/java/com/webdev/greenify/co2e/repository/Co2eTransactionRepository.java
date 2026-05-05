package com.webdev.greenify.co2e.repository;

import com.webdev.greenify.co2e.entity.Co2eTransactionEntity;
import com.webdev.greenify.co2e.enumeration.Co2eTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    Optional<Co2eTransactionEntity> findByPostId(String postId);
}
