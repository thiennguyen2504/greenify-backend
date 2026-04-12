package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.PostReviewEntity;
import com.webdev.greenify.greenaction.enumeration.ReviewDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostReviewRepository extends JpaRepository<PostReviewEntity, String> {

    /**
     * Check if reviewer has already submitted a valid review for this post.
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM PostReviewEntity r
            WHERE r.post.id = :postId
            AND r.reviewer.id = :reviewerId
            AND r.isValid = true
            """)
    boolean existsByPostIdAndReviewerIdAndIsValidTrue(
            @Param("postId") String postId,
            @Param("reviewerId") String reviewerId);

    /**
     * Find all valid reviews for a post with a specific decision.
     * Used to award points to all reviewers who approved a verified post.
     */
    @Query("""
            SELECT r FROM PostReviewEntity r
            JOIN FETCH r.reviewer
            WHERE r.post.id = :postId
            AND r.decision = :decision
            AND r.isValid = true
            """)
    List<PostReviewEntity> findByPostIdAndDecisionAndIsValidTrue(
            @Param("postId") String postId,
            @Param("decision") ReviewDecision decision);

    /**
     * Check if a post has any valid review with a specific decision.
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM PostReviewEntity r
            WHERE r.post.id = :postId
            AND r.decision = :decision
            AND r.isValid = true
            """)
    boolean existsByPostIdAndDecisionAndIsValidTrue(
            @Param("postId") String postId,
            @Param("decision") ReviewDecision decision);
}
