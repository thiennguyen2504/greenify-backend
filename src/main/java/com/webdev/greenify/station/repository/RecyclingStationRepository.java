package com.webdev.greenify.station.repository;

import com.webdev.greenify.station.entity.RecyclingStationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecyclingStationRepository extends JpaRepository<RecyclingStationEntity, String> {

    @EntityGraph(attributePaths = { "address", "wasteTypes" })
    @Query("""
            SELECT DISTINCT rs
            FROM RecyclingStationEntity rs
            LEFT JOIN rs.wasteTypes wt
            WHERE rs.isDeleted = false
            AND (:wasteTypeId IS NULL OR wt.id = :wasteTypeId)
            """)
    List<RecyclingStationEntity> findAllActiveByWasteTypeId(@Param("wasteTypeId") String wasteTypeId);

    @EntityGraph(attributePaths = { "address", "wasteTypes" })
    Optional<RecyclingStationEntity> findByIdAndIsDeletedFalse(String id);
}
