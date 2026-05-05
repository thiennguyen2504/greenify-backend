package com.webdev.greenify.co2e.repository;

import com.webdev.greenify.co2e.entity.GreenImpactWalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for Green Impact Wallet aggregates.
 */
public interface GreenImpactWalletRepository extends JpaRepository<GreenImpactWalletEntity, String> {

    Optional<GreenImpactWalletEntity> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM GreenImpactWalletEntity w
            WHERE w.userId = :userId
            """)
    Optional<GreenImpactWalletEntity> findByUserIdForUpdate(@Param("userId") String userId);
}
