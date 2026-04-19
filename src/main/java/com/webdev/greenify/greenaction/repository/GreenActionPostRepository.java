package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GreenActionPostRepository extends JpaRepository<GreenActionPostEntity, String>,
        JpaSpecificationExecutor<GreenActionPostEntity> {

        long countByUser_IdAndIsDeletedFalse(String userId);

    /**
     * Fetch top posts with JOIN FETCH to prevent N+1 queries.
     * Only returns VERIFIED posts, ordered by approveCount DESC, then createdAt DESC.
     */
    @Query("""
            SELECT p FROM GreenActionPostEntity p
            JOIN FETCH p.user u
            JOIN FETCH p.actionType at
            WHERE p.status = :status
            ORDER BY p.approveCount DESC, p.createdAt DESC
            """)
    List<GreenActionPostEntity> findTopPostsWithUserAndActionType(
            @Param("status") PostStatus status,
            Pageable pageable);

    /**
     * Find post by ID with user and actionType eagerly loaded.
     */
    @Query("""
            SELECT p FROM GreenActionPostEntity p
            JOIN FETCH p.user u
            JOIN FETCH p.actionType at
            WHERE p.id = :id
            """)
    Optional<GreenActionPostEntity> findByIdWithUserAndActionType(@Param("id") String id);

    /**
     * For Specification-based queries with EntityGraph to prevent N+1.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "actionType"})
    Page<GreenActionPostEntity> findAll(Specification<GreenActionPostEntity> spec, Pageable pageable);

        @EntityGraph(attributePaths = {"actionType", "postImage"})
        List<GreenActionPostEntity> findByIdIn(Collection<String> ids);


        @Query("""
                SELECT COUNT(p)
                FROM GreenActionPostEntity p
                WHERE p.status = :status
                AND p.createdAt BETWEEN :start AND :end
                AND p.isDeleted = false
                """)
        long countByCreatedAtBetweenAndStatus(
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end,
                @Param("status") PostStatus status);
}
