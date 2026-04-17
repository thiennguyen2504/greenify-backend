package com.webdev.greenify.voucher.repository;

import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface VoucherTemplateRepository extends JpaRepository<VoucherTemplateEntity, String> {

    Page<VoucherTemplateEntity> findByStatusAndRemainingStockGreaterThanAndValidUntilAfter(
            VoucherTemplateStatus status,
            Integer remainingStock,
            LocalDateTime validUntil,
            Pageable pageable);

    @Query("""
            SELECT vt
            FROM VoucherTemplateEntity vt
            WHERE vt.status = :status
            AND vt.remainingStock > 0
            AND vt.validUntil > :currentTime
            AND (:minRequiredPoints IS NULL OR vt.requiredPoints >= :minRequiredPoints)
            AND (:maxRequiredPoints IS NULL OR vt.requiredPoints <= :maxRequiredPoints)
            """)
    Page<VoucherTemplateEntity> findMarketplaceVouchersByRequiredPoints(
            @Param("status") VoucherTemplateStatus status,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("minRequiredPoints") BigDecimal minRequiredPoints,
            @Param("maxRequiredPoints") BigDecimal maxRequiredPoints,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT vt
            FROM VoucherTemplateEntity vt
            WHERE vt.id = :voucherTemplateId
            """)
    Optional<VoucherTemplateEntity> findByIdForUpdate(@Param("voucherTemplateId") String voucherTemplateId);

    @Modifying
    @Query("""
            UPDATE VoucherTemplateEntity vt
            SET vt.remainingStock = vt.remainingStock - :count
            WHERE vt.id = :id
            AND vt.remainingStock >= :count
            """)
    int decreaseRemainingStockBatch(@Param("id") String id, @Param("count") int count);
}