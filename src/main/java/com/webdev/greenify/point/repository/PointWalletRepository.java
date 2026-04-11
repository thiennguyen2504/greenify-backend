package com.webdev.greenify.point.repository;

import com.webdev.greenify.point.entity.PointWalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}