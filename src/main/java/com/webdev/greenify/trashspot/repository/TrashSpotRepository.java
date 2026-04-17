package com.webdev.greenify.trashspot.repository;

import com.webdev.greenify.trashspot.entity.TrashSpotEntity;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;
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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrashSpotRepository extends JpaRepository<TrashSpotEntity, String>, JpaSpecificationExecutor<TrashSpotEntity> {

    @Override
    Page<TrashSpotEntity> findAll(Specification<TrashSpotEntity> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"images", "wasteTypes"})
    List<TrashSpotEntity> findByIdIn(Collection<String> ids);

    @EntityGraph(attributePaths = {"reporter", "assignedNgo", "images", "wasteTypes"})
    Optional<TrashSpotEntity> findByIdAndIsDeletedFalse(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ts
            FROM TrashSpotEntity ts
            WHERE ts.id = :id
            AND ts.isDeleted = false
            """)
    Optional<TrashSpotEntity> findByIdForUpdate(@Param("id") String id);

    @Query(value = """
            SELECT ts.*
            FROM trash_spots ts
            WHERE ts.is_deleted = false
              AND ts.status <> :resolvedStatus
              AND (6371000 * acos(
                    cos(radians(CAST(:latitude AS double precision)))
                    * cos(radians(CAST(ts.latitude AS double precision)))
                    * cos(radians(CAST(ts.longitude AS double precision)) - radians(CAST(:longitude AS double precision)))
                    + sin(radians(CAST(:latitude AS double precision)))
                    * sin(radians(CAST(ts.latitude AS double precision)))
              )) <= :radiusMeters
            ORDER BY (6371000 * acos(
                    cos(radians(CAST(:latitude AS double precision)))
                    * cos(radians(CAST(ts.latitude AS double precision)))
                    * cos(radians(CAST(ts.longitude AS double precision)) - radians(CAST(:longitude AS double precision)))
                    + sin(radians(CAST(:latitude AS double precision)))
                    * sin(radians(CAST(ts.latitude AS double precision)))
            )) ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<TrashSpotEntity> findNearestNonResolvedWithinRadius(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("resolvedStatus") String resolvedStatus);

    @Query(value = """
            SELECT ts.*
            FROM trash_spots ts
            WHERE ts.is_deleted = false
              AND ts.status = :resolvedStatus
              AND (6371000 * acos(
                    cos(radians(CAST(:latitude AS double precision)))
                    * cos(radians(CAST(ts.latitude AS double precision)))
                    * cos(radians(CAST(ts.longitude AS double precision)) - radians(CAST(:longitude AS double precision)))
                    + sin(radians(CAST(:latitude AS double precision)))
                    * sin(radians(CAST(ts.latitude AS double precision)))
              )) <= :radiusMeters
            ORDER BY (6371000 * acos(
                    cos(radians(CAST(:latitude AS double precision)))
                    * cos(radians(CAST(ts.latitude AS double precision)))
                    * cos(radians(CAST(ts.longitude AS double precision)) - radians(CAST(:longitude AS double precision)))
                    + sin(radians(CAST(:latitude AS double precision)))
                    * sin(radians(CAST(ts.latitude AS double precision)))
            )) ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<TrashSpotEntity> findNearestResolvedWithinRadius(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("resolvedStatus") String resolvedStatus);

    @EntityGraph(attributePaths = {"images", "wasteTypes"})
    List<TrashSpotEntity> findByStatusInAndIsDeletedFalse(Collection<TrashSpotStatus> statuses);

        @Query("""
                        SELECT ts
                        FROM TrashSpotEntity ts
                        WHERE ts.isDeleted = false
                        AND ts.status IN :statuses
                        """)
        List<TrashSpotEntity> findActiveForHotScoreRecalculation(@Param("statuses") Collection<TrashSpotStatus> statuses);
}
