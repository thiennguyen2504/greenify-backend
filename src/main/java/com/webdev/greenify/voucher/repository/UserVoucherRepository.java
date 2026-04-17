package com.webdev.greenify.voucher.repository;

import com.webdev.greenify.voucher.entity.UserVoucherEntity;
import com.webdev.greenify.voucher.enumeration.UserVoucherStatus;
import com.webdev.greenify.voucher.enumeration.VoucherSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserVoucherRepository extends JpaRepository<UserVoucherEntity, String> {

    boolean existsByVoucherCode(String voucherCode);

    @EntityGraph(attributePaths = {"voucherTemplate"})
    @Query("""
            SELECT uv
            FROM UserVoucherEntity uv
            WHERE uv.user.id = :userId
            AND uv.status = :status
            ORDER BY uv.createdAt DESC
            """)
    Page<UserVoucherEntity> findByUserIdAndStatus(
            @Param("userId") String userId,
            @Param("status") UserVoucherStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"voucherTemplate"})
    @Query("""
            SELECT uv
            FROM UserVoucherEntity uv
            WHERE uv.user.id = :userId
            ORDER BY uv.createdAt DESC
            """)
    Page<UserVoucherEntity> findByUserId(
            @Param("userId") String userId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"voucherTemplate"})
    @Query("""
            SELECT uv
            FROM UserVoucherEntity uv
            WHERE uv.user.id = :userId
            AND (:status IS NULL OR uv.status = :status)
            AND (:source IS NULL OR uv.source = :source)
            ORDER BY uv.createdAt DESC
            """)
    Page<UserVoucherEntity> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("status") UserVoucherStatus status,
            @Param("source") VoucherSource source,
            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE UserVoucherEntity uv
            SET uv.status = :expiredStatus
            WHERE uv.user.id = :userId
            AND uv.status = :availableStatus
            AND uv.expiresAt < :currentTime
            """)
    int expireAvailableVouchers(
            @Param("userId") String userId,
            @Param("availableStatus") UserVoucherStatus availableStatus,
            @Param("expiredStatus") UserVoucherStatus expiredStatus,
            @Param("currentTime") LocalDateTime currentTime);
}