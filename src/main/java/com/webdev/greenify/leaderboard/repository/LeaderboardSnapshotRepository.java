package com.webdev.greenify.leaderboard.repository;

import com.webdev.greenify.leaderboard.entity.LeaderboardSnapshotEntity;
import com.webdev.greenify.leaderboard.enumeration.LeaderboardScope;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshotEntity, String> {

    boolean existsByPrizeConfigId(String prizeConfigId);

    @Query("""
            SELECT ls.id
            FROM LeaderboardSnapshotEntity ls
            WHERE ls.prizeConfig.id = :prizeConfigId
            AND ls.rewarded = false
            AND ls.isDeleted = false
            ORDER BY ls.scope ASC, ls.province ASC, ls.rank ASC
            """)
    List<String> findPendingSnapshotIds(@Param("prizeConfigId") String prizeConfigId);

    @EntityGraph(attributePaths = {"user", "user.userProfile", "user.userProfile.avatar"})
    @Query("""
            SELECT ls
            FROM LeaderboardSnapshotEntity ls
            WHERE ls.weekStartDate = :weekStartDate
            AND ls.scope = :scope
            AND ls.isDeleted = false
            ORDER BY ls.rank ASC
            """)
    List<LeaderboardSnapshotEntity> findByWeekStartDateAndScopeOrderByRankAsc(
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("scope") LeaderboardScope scope);

    @EntityGraph(attributePaths = {"user", "user.userProfile", "user.userProfile.avatar"})
    @Query("""
            SELECT ls
            FROM LeaderboardSnapshotEntity ls
            WHERE ls.weekStartDate = :weekStartDate
            AND ls.scope = :scope
            AND ls.province = :province
            AND ls.isDeleted = false
            ORDER BY ls.rank ASC
            """)
    List<LeaderboardSnapshotEntity> findByWeekStartDateAndScopeAndProvinceOrderByRankAsc(
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("scope") LeaderboardScope scope,
            @Param("province") String province);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "user",
            "prizeConfig",
            "prizeConfig.nationalVoucherTemplate",
            "prizeConfig.provincialVoucherTemplate"
    })
    @Query("""
            SELECT ls
            FROM LeaderboardSnapshotEntity ls
            WHERE ls.id = :id
            AND ls.isDeleted = false
            """)
    Optional<LeaderboardSnapshotEntity> findByIdForUpdate(@Param("id") String id);
}
