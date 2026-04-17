package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.dto.response.PointHistoryResponse;
import com.webdev.greenify.greenaction.dto.response.TotalPointsResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import com.webdev.greenify.user.entity.UserEntity;

import java.math.BigDecimal;

public interface PointService {

    /**
     * Award points to user for a verified green action post.
     */
    PointTransactionEntity awardPointsToPostAuthor(
            UserEntity user,
            GreenActionPostEntity post,
            BigDecimal points);

    /**
     * Award points to reviewer/CTV for approving a valid post.
     */
    PointTransactionEntity awardPointsToReviewer(
            UserEntity reviewer,
            String reviewId,
            GreenActionPostEntity post);

    /**
     * Get total points for current user (accumulated and available).
     */
    TotalPointsResponse getTotalPointsForCurrentUser();

    /**
     * Get paginated point history for current user.
     */
    PagedResponse<PointHistoryResponse> getPointHistoryForCurrentUser(int page, int size);

    /**
     * Process expired points and create deduction transactions.
     * Called by scheduled job.
     */
    void processExpiredPoints();

    /**
     * Invalidate lazily-loaded action-type derived caches.
     */
    void invalidateActionTypeCache();
}
