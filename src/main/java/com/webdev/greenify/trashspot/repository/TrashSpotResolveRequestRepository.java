package com.webdev.greenify.trashspot.repository;

import com.webdev.greenify.trashspot.entity.TrashSpotResolveRequestEntity;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrashSpotResolveRequestRepository extends JpaRepository<TrashSpotResolveRequestEntity, String> {

    @EntityGraph(attributePaths = {"ngo", "reviewedBy", "images"})
    List<TrashSpotResolveRequestEntity> findByTrashSpotIdAndIsDeletedFalseOrderByCreatedAtDesc(String trashSpotId);

    @EntityGraph(attributePaths = {"trashSpot", "ngo", "reviewedBy", "images"})
    Optional<TrashSpotResolveRequestEntity> findByIdAndIsDeletedFalse(String id);

    @EntityGraph(attributePaths = {"trashSpot", "ngo", "reviewedBy"})
    Page<TrashSpotResolveRequestEntity> findByStatusAndIsDeletedFalse(ResolveRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"trashSpot", "ngo", "reviewedBy"})
    Page<TrashSpotResolveRequestEntity> findByIsDeletedFalse(Pageable pageable);


    @Query("""
            SELECT COUNT(r)
            FROM TrashSpotResolveRequestEntity r
            WHERE r.status = :status
            AND r.reviewedAt BETWEEN :start AND :end
            AND r.isDeleted = false
            """)
    long countByReviewedAtBetweenAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") ResolveRequestStatus status);
}
