package com.webdev.greenify.trashspot.repository;

import com.webdev.greenify.trashspot.entity.TrashSpotVerificationEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrashSpotVerificationRepository extends JpaRepository<TrashSpotVerificationEntity, String> {

    boolean existsByTrashSpotIdAndVerifierIdAndIsDeletedFalse(String trashSpotId, String verifierId);

    @EntityGraph(attributePaths = {"verifier"})
    List<TrashSpotVerificationEntity> findByTrashSpotIdAndIsDeletedFalseOrderByCreatedAtDesc(String trashSpotId);

    @Modifying
    @Query("""
            DELETE FROM TrashSpotVerificationEntity v
            WHERE v.trashSpot.id = :trashSpotId
            """)
    int deleteByTrashSpotId(@Param("trashSpotId") String trashSpotId);
}
