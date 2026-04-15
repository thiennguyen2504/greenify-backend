package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.PostAppealEntity;
import com.webdev.greenify.greenaction.enumeration.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostAppealRepository extends JpaRepository<PostAppealEntity, String> {

    long countByPost_IdAndUser_Id(String postId, String userId);

    boolean existsByPost_IdAndUser_IdAndStatus(String postId, String userId, AppealStatus status);

    @EntityGraph(attributePaths = {"post", "user"})
    Page<PostAppealEntity> findByStatusOrderByCreatedAtDesc(AppealStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "user"})
    Page<PostAppealEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"post", "user"})
    @Query("""
            SELECT a
            FROM PostAppealEntity a
            WHERE a.id = :appealId
            """)
    Optional<PostAppealEntity> findByIdWithPostAndUser(@Param("appealId") String appealId);
}
