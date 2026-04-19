package com.webdev.greenify.trashspot.repository;

import com.webdev.greenify.trashspot.entity.TrashSpotReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrashSpotReportRepository extends JpaRepository<TrashSpotReportEntity, String> {

    boolean existsByTrashSpotIdAndReporterIdAndIsDeletedFalse(String trashSpotId, String reporterId);

    @EntityGraph(attributePaths = {
            "trashSpot",
            "trashSpot.images",
            "trashSpot.wasteTypes",
            "reporter",
            "reporter.userProfile",
            "reporter.userProfile.avatar"
    })
    Page<TrashSpotReportEntity> findByIsDeletedFalse(Pageable pageable);
}