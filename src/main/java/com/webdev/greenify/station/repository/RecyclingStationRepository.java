package com.webdev.greenify.station.repository;

import com.webdev.greenify.station.entity.RecyclingStationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecyclingStationRepository extends JpaRepository<RecyclingStationEntity, String> {
    @EntityGraph(attributePaths = { "address", "wasteTypes" })
    Page<RecyclingStationEntity> findByIsDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = { "address", "wasteTypes" })
    Optional<RecyclingStationEntity> findByIdAndIsDeletedFalse(String id);
}
