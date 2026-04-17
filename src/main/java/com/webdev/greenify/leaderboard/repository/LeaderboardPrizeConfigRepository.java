package com.webdev.greenify.leaderboard.repository;

import com.webdev.greenify.leaderboard.entity.LeaderboardPrizeConfigEntity;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardPrizeConfigRepository
        extends JpaRepository<LeaderboardPrizeConfigEntity, String>, JpaSpecificationExecutor<LeaderboardPrizeConfigEntity> {

    @EntityGraph(attributePaths = {"nationalVoucherTemplate", "provincialVoucherTemplate"})
    Optional<LeaderboardPrizeConfigEntity> findByIdAndIsDeletedFalse(String id);

    @EntityGraph(attributePaths = {"nationalVoucherTemplate", "provincialVoucherTemplate"})
    Optional<LeaderboardPrizeConfigEntity> findByWeekStartDate(LocalDate weekStartDate);

    @EntityGraph(attributePaths = {"nationalVoucherTemplate", "provincialVoucherTemplate"})
    Optional<LeaderboardPrizeConfigEntity> findByWeekStartDateAndIsDeletedFalse(LocalDate weekStartDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"nationalVoucherTemplate", "provincialVoucherTemplate"})
    @Query("""
            SELECT lpc
            FROM LeaderboardPrizeConfigEntity lpc
            WHERE lpc.id = :id
            AND lpc.isDeleted = false
            """)
    Optional<LeaderboardPrizeConfigEntity> findByIdForUpdate(@Param("id") String id);

    @Query("""
            SELECT lpc.id
            FROM LeaderboardPrizeConfigEntity lpc
            WHERE lpc.status = :status
            AND lpc.lockAt <= :lockAt
            AND lpc.isDeleted = false
            ORDER BY lpc.lockAt ASC
            """)
    List<String> findIdsToFinalize(@Param("status") PrizeConfigStatus status, @Param("lockAt") LocalDateTime lockAt);

    @Override
    @EntityGraph(attributePaths = {"nationalVoucherTemplate", "provincialVoucherTemplate"})
    Page<LeaderboardPrizeConfigEntity> findAll(Specification<LeaderboardPrizeConfigEntity> spec, Pageable pageable);
}
