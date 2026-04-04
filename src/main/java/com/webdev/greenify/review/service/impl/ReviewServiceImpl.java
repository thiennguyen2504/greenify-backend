package com.webdev.greenify.review.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostReviewerResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.mapper.GreenActionMapper;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.review.dto.request.SubmitReviewRequest;
import com.webdev.greenify.review.dto.response.SubmitReviewResponse;
import com.webdev.greenify.review.entity.PostReviewEntity;
import com.webdev.greenify.review.enumeration.ReviewDecision;
import com.webdev.greenify.review.mapper.ReviewMapper;
import com.webdev.greenify.review.repository.PostReviewRepository;
import com.webdev.greenify.review.service.ReviewService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private static final Set<PostStatus> REVIEWABLE_STATUSES = Set.of(
            PostStatus.PENDING_REVIEW, PostStatus.PARTIALLY_APPROVED);
    
    private static final int APPROVE_THRESHOLD = 3;
    private static final int REJECT_THRESHOLD = 5;

    private final GreenActionPostRepository postRepository;
    private final PostReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GreenActionMapper greenActionMapper;
    private final ReviewMapper reviewMapper;

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
            (request.getRejectReasonCode() == null || request.getRejectReasonCode().isBlank())) {
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
                .rejectReasonCode(request.getRejectReasonCode())
                .rejectReasonNote(request.getRejectReasonNote())
                .isValid(true)
                .build();

        review = reviewRepository.save(review);

        // Update post counts and status atomically
        PostStatus newStatus = updatePostStatusAfterReview(post, request.getDecision());
        post = postRepository.save(post);

        log.info("Review submitted: reviewId={}, postId={}, decision={}, newStatus={}", 
                review.getId(), postId, request.getDecision(), newStatus);

        return reviewMapper.toSubmitReviewResponse(review, request.getDecision(), newStatus);
    }

    /**
     * Update post counts and determine new status based on review decision.
     * Must be called within the same transaction as review save.
     */
    private PostStatus updatePostStatusAfterReview(GreenActionPostEntity post, ReviewDecision decision) {
        PostStatus newStatus;

        // Normalize null counts to 0
        int currentApproveCount = post.getApproveCount() != null ? post.getApproveCount() : 0;
        int currentRejectCount = post.getRejectCount() != null ? post.getRejectCount() : 0;

        switch (decision) {
            case APPROVE -> {
                int newApproveCount = currentApproveCount + 1;
                post.setApproveCount(newApproveCount);
                
                if (newApproveCount >= APPROVE_THRESHOLD) {
                    newStatus = PostStatus.VERIFIED;
                    log.info("Post {} auto-approved with {} approvals", post.getId(), newApproveCount);
                } else {
                    newStatus = PostStatus.PARTIALLY_APPROVED;
                }
            }
            case REJECT -> {
                int newRejectCount = currentRejectCount + 1;
                post.setRejectCount(newRejectCount);
                
                if (newRejectCount >= REJECT_THRESHOLD) {
                    newStatus = PostStatus.FLAGGED;
                    log.info("Post {} auto-flagged with {} rejections", post.getId(), newRejectCount);
                } else {
                    newStatus = PostStatus.PARTIALLY_APPROVED;
                }
            }
            case REPORT_SUSPICIOUS -> {
                // Immediate flagging for suspicious reports
                newStatus = PostStatus.FLAGGED;
                post.setRejectCount(currentRejectCount + 1);
                log.warn("Post {} flagged as suspicious by reviewer", post.getId());
            }
            default -> throw new IllegalArgumentException("Unknown decision type: " + decision);
        }

        post.setStatus(newStatus);
        return newStatus;
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
