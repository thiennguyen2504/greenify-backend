package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.dto.request.SubmitReviewRequest;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostReviewerResponse;
import com.webdev.greenify.greenaction.dto.response.SubmitReviewResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.PostReviewEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.enumeration.ReviewDecision;
import com.webdev.greenify.greenaction.mapper.GreenActionMapper;
import com.webdev.greenify.greenaction.mapper.ReviewMapper;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.PostReviewRepository;
import com.webdev.greenify.greenaction.service.PointService;
import com.webdev.greenify.greenaction.service.ReviewService;
import com.webdev.greenify.streak.service.StreakService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private static final Set<PostStatus> REVIEWABLE_STATUSES = Set.of(
            PostStatus.PENDING_REVIEW, PostStatus.PARTIALLY_APPROVED);

    private static final int APPROVE_THRESHOLD = 3;
    private static final int REJECT_THRESHOLD = 3;
    private static final double DECISION_RATIO_THRESHOLD = 0.6;

    private final GreenActionPostRepository postRepository;
    private final PostReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GreenActionMapper greenActionMapper;
    private final ReviewMapper reviewMapper;
    private final PointService pointService;
    private final StreakService streakService;

    @Override
    @Transactional(readOnly = true)
    public GreenActionPostReviewerResponse getPostForReview(String postId) {
        String currentUserId = getCurrentUserId();
        
        // 1. Post must exist
        GreenActionPostEntity post = postRepository.findByIdWithUserAndActionType(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // 2. Post status must be reviewable
        if (!REVIEWABLE_STATUSES.contains(post.getStatus())) {
            throw new AppException("Bài viết không trong trạng thái chờ duyệt", HttpStatus.BAD_REQUEST);
        }

        // 3. Self-review guard
        if (post.getUser().getId().equals(currentUserId)) {
            throw new AppException("Không thể duyệt bài của chính mình", HttpStatus.FORBIDDEN);
        }

        // 4. Already-reviewed guard
        boolean alreadyReviewed = reviewRepository.existsByPostIdAndReviewerIdAndIsValidTrue(postId, currentUserId);
        if (alreadyReviewed) {
            throw new AppException("Bạn đã duyệt bài này rồi", HttpStatus.CONFLICT);
        }

        GreenActionPostReviewerResponse response = greenActionMapper.toReviewerResponse(post);
        response.setAlreadyReviewed(false);
        response.setReviews(reviewMapper.toPostReviewResponseList(
                reviewRepository.findByPostIdAndIsValidTrueOrderByCreatedAtDesc(postId)));
        response.setLocation(buildMockLocation(post.getLatitude(), post.getLongitude()));
        return response;
    }

    @Override
    @Transactional
    public SubmitReviewResponse submitReview(String postId, SubmitReviewRequest request) {
        String currentUserId = getCurrentUserId();

        // 1. Post must exist
        GreenActionPostEntity post = postRepository.findByIdWithUserAndActionType(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // 2. Post status must be reviewable
        if (!REVIEWABLE_STATUSES.contains(post.getStatus())) {
            throw new AppException("Bài viết không trong trạng thái chờ duyệt", HttpStatus.BAD_REQUEST);
        }

        // 3. Self-review guard
        if (post.getUser().getId().equals(currentUserId)) {
            throw new AppException("Không thể duyệt bài của chính mình", HttpStatus.FORBIDDEN);
        }

        // 4. Already-reviewed guard
        boolean alreadyReviewed = reviewRepository.existsByPostIdAndReviewerIdAndIsValidTrue(postId, currentUserId);
        if (alreadyReviewed) {
            throw new AppException("Bạn đã duyệt bài này rồi", HttpStatus.CONFLICT);
        }

        // 5. Reject reason required for REJECT or REPORT_SUSPICIOUS
        if ((request.getDecision() == ReviewDecision.REJECT || 
             request.getDecision() == ReviewDecision.REPORT_SUSPICIOUS) &&
            (request.getRejectReason() == null || request.getRejectReason().isBlank())) {
            throw new AppException("Lý do từ chối là bắt buộc", HttpStatus.BAD_REQUEST);
        }

        // Get current user
        UserEntity reviewer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        // Create review record
        PostReviewEntity review = PostReviewEntity.builder()
                .post(post)
                .reviewer(reviewer)
                .decision(request.getDecision())
                .rejectReason(request.getRejectReason())
                .isValid(true)
                .build();

        review = reviewRepository.save(review);

        // Update post counts and status atomically
        boolean adminDirectReject = isCurrentUserAdmin() && request.getDecision() == ReviewDecision.REJECT;
        PostStatus newStatus = updatePostStatusAfterReview(post, request.getDecision(), adminDirectReject);
        post = postRepository.save(post);

        // Award points if post becomes VERIFIED
        if (newStatus == PostStatus.VERIFIED) {
            streakService.handleVerifiedPost(
                    post.getUser().getId(),
                    post.getActionDate(),
                    post.getPostImage() != null ? post.getPostImage().getImageUrl() : null);
            awardPointsForVerifiedPost(post);
        }

        log.info("Review submitted: reviewId={}, postId={}, decision={}, newStatus={}", 
                review.getId(), postId, request.getDecision(), newStatus);

        return reviewMapper.toSubmitReviewResponse(review, request.getDecision(), newStatus);
    }

    /**
     * Award points to post author and all approving reviewers when a post is verified.
     */
    private void awardPointsForVerifiedPost(GreenActionPostEntity post) {
        // 1. Award points to post author based on action type's suggestedPoints
        BigDecimal postPoints = post.getActionType().getSuggestedPoints();
        if (postPoints != null && postPoints.compareTo(BigDecimal.ZERO) > 0) {
            pointService.awardPointsToPostAuthor(post.getUser(), post, postPoints);
            log.info("Awarded {} points to post author {} for verified post {}",
                    postPoints, post.getUser().getId(), post.getId());
        }

        // 2. Award points to all reviewers who approved the post
        List<PostReviewEntity> approvingReviews = reviewRepository
                .findByPostIdAndDecisionAndIsValidTrue(post.getId(), ReviewDecision.APPROVE);

        for (PostReviewEntity review : approvingReviews) {
            pointService.awardPointsToReviewer(review.getReviewer(), review.getId(), post);
        }
        
        log.info("Awarded points to {} reviewers for verified post {}",
                approvingReviews.size(), post.getId());
    }

    /**
     * Update post counts and determine new status based on review decision.
     * Must be called within the same transaction as review save.
     */
    private PostStatus updatePostStatusAfterReview(
            GreenActionPostEntity post,
            ReviewDecision decision,
            boolean adminDirectReject) {
        PostStatus newStatus;

        // Normalize null counts to 0
        int currentApproveCount = post.getApproveCount() != null ? post.getApproveCount() : 0;
        int currentRejectCount = post.getRejectCount() != null ? post.getRejectCount() : 0;

        int newApproveCount = currentApproveCount;
        int newRejectCount = currentRejectCount;

        switch (decision) {
            case APPROVE -> newApproveCount++;
            case REJECT, REPORT_SUSPICIOUS -> newRejectCount++;
            default -> throw new IllegalArgumentException("Unknown decision type: " + decision);
        }

        post.setApproveCount(newApproveCount);
        post.setRejectCount(newRejectCount);

        int totalReviews = newApproveCount + newRejectCount;
        double approveRatio = totalReviews == 0 ? 0 : (double) newApproveCount / totalReviews;
        double rejectRatio = totalReviews == 0 ? 0 : (double) newRejectCount / totalReviews;

        boolean hasSeriousFraudFlag = decision == ReviewDecision.REPORT_SUSPICIOUS
                || reviewRepository.existsByPostIdAndDecisionAndIsValidTrue(post.getId(), ReviewDecision.REPORT_SUSPICIOUS);

        boolean isVerified = newApproveCount >= APPROVE_THRESHOLD
                && approveRatio >= DECISION_RATIO_THRESHOLD
                && !hasSeriousFraudFlag;

        boolean isRejectedByVotes = newRejectCount >= REJECT_THRESHOLD
                && rejectRatio >= DECISION_RATIO_THRESHOLD;

        if (adminDirectReject || isRejectedByVotes) {
            newStatus = PostStatus.REJECTED;
            log.info("Post {} rejected: adminDirectReject={}, rejectCount={}, totalReviews={}, rejectRatio={}",
                    post.getId(), adminDirectReject, newRejectCount, totalReviews, rejectRatio);
        } else if (isVerified) {
            newStatus = PostStatus.VERIFIED;
            log.info("Post {} verified with approveCount={}, totalReviews={}, approveRatio={}",
                    post.getId(), newApproveCount, totalReviews, approveRatio);
        } else {
            newStatus = PostStatus.PENDING_REVIEW;
        }

        post.setStatus(newStatus);
        return newStatus;
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String buildMockLocation(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return "Vi tri mo phong: chua co toa do";
        }
        return "Vi tri mo phong tu toa do ("
                + latitude.stripTrailingZeros().toPlainString()
                + ", "
                + longitude.stripTrailingZeros().toPlainString()
                + ")";
    }
}
