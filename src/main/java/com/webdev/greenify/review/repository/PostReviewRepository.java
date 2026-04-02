package com.webdev.greenify.review.repository;

import com.webdev.greenify.review.entity.PostReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
