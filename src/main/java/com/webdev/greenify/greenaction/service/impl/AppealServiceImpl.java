package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.dto.request.CreateAppealRequest;
import com.webdev.greenify.greenaction.dto.request.ReviewAppealRequest;
import com.webdev.greenify.greenaction.dto.request.UpdateAppealRequest;
import com.webdev.greenify.greenaction.dto.response.AppealResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.PostAppealEntity;
import com.webdev.greenify.greenaction.enumeration.AppealStatus;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.PostAppealRepository;
import com.webdev.greenify.greenaction.repository.PostReviewRepository;
import com.webdev.greenify.greenaction.service.AppealService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppealServiceImpl implements AppealService {

    private static final int MAX_APPEAL_ATTEMPTS = 2;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostAppealRepository postAppealRepository;
    private final GreenActionPostRepository postRepository;
    private final PostReviewRepository postReviewRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AppealResponse createAppeal(CreateAppealRequest request) {
        String userId = getCurrentUserId();

        GreenActionPostEntity post = postRepository.findByIdWithUserAndActionType(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết"));

        if (!post.getUser().getId().equals(userId)) {
            throw new AppException("Bạn chỉ có thể khiếu nại bài viết của chính mình", HttpStatus.FORBIDDEN);
        }

        if (post.getStatus() != PostStatus.REJECTED) {
            throw new AppException("Chỉ có thể khiếu nại khi bài viết ở trạng thái REJECTED", HttpStatus.BAD_REQUEST);
        }

        long attempts = postAppealRepository.countByPost_IdAndUser_Id(post.getId(), userId);
        if (attempts >= MAX_APPEAL_ATTEMPTS) {
            throw new AppException("Mỗi bài chỉ được khiếu nại tối đa 2 lần", HttpStatus.BAD_REQUEST);
        }

        boolean hasPendingAppeal = postAppealRepository.existsByPost_IdAndUser_IdAndStatus(
                post.getId(), userId, AppealStatus.APPEAL_SUBMITTED);

        if (hasPendingAppeal) {
            throw new AppException("Bạn đã có khiếu nại đang chờ xử lý cho bài viết này", HttpStatus.CONFLICT);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        PostAppealEntity appeal = PostAppealEntity.builder()
                .post(post)
                .user(user)
                .appealReason(request.getAppealReason().trim())
                .evidenceUrls(normalizeEvidenceUrls(request.getEvidenceUrls()))
                .attemptNumber((int) attempts + 1)
                .status(AppealStatus.APPEAL_SUBMITTED)
                .build();

        appeal = postAppealRepository.save(appeal);
        log.info("Created appeal {} for post {} by user {} (attempt #{})",
                appeal.getId(), post.getId(), userId, appeal.getAttemptNumber());

        return toResponse(appeal);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AppealResponse> getAppealsForReview(AppealStatus status, int page, int size) {
        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(
                effectivePage,
                effectiveSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostAppealEntity> appealsPage = status == null
                ? postAppealRepository.findAllByOrderByCreatedAtDesc(pageable)
                : postAppealRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        List<AppealResponse> content = appealsPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.of(
                content,
                appealsPage.getNumber(),
                appealsPage.getSize(),
                appealsPage.getTotalElements(),
                appealsPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public AppealResponse getAppealDetail(String appealId) {
        PostAppealEntity appeal = findAppealOrThrow(appealId);
        String currentUserId = getCurrentUserId();

        if (!isCurrentUserAdmin() && !appeal.getUser().getId().equals(currentUserId)) {
            throw new AppException("Bạn không có quyền xem khiếu nại này", HttpStatus.FORBIDDEN);
        }

        return toResponse(appeal);
    }

    @Override
    @Transactional
    public AppealResponse updateAppeal(String appealId, UpdateAppealRequest request) {
        PostAppealEntity appeal = findAppealOrThrow(appealId);
        String currentUserId = getCurrentUserId();

        if (!appeal.getUser().getId().equals(currentUserId)) {
            throw new AppException("Bạn chỉ có thể cập nhật khiếu nại của chính mình", HttpStatus.FORBIDDEN);
        }

        if (appeal.getStatus() != AppealStatus.APPEAL_SUBMITTED) {
            throw new AppException("Chỉ có thể cập nhật khiếu nại đang ở trạng thái APPEAL_SUBMITTED", HttpStatus.BAD_REQUEST);
        }

        appeal.setAppealReason(request.getAppealReason().trim());
        appeal.setEvidenceUrls(normalizeEvidenceUrls(request.getEvidenceUrls()));

        appeal = postAppealRepository.save(appeal);
        log.info("Updated appeal {} by user {}", appealId, currentUserId);

        return toResponse(appeal);
    }

    @Override
    @Transactional
    public AppealResponse reviewAppeal(String appealId, ReviewAppealRequest request) {
        PostAppealEntity appeal = findAppealOrThrow(appealId);

        if (appeal.getStatus() != AppealStatus.APPEAL_SUBMITTED) {
            throw new AppException("Khiếu nại này đã được xử lý", HttpStatus.BAD_REQUEST);
        }

        if (request.getStatus() != AppealStatus.APPEAL_ACCEPTED
                && request.getStatus() != AppealStatus.APPEAL_REJECTED) {
            throw new AppException("Trạng thái review phải là APPEAL_ACCEPTED hoặc APPEAL_REJECTED", HttpStatus.BAD_REQUEST);
        }

        appeal.setStatus(request.getStatus());
        appeal.setAdminNote(request.getAdminNote());

        if (request.getStatus() == AppealStatus.APPEAL_ACCEPTED) {
            GreenActionPostEntity post = appeal.getPost();
            post.setStatus(PostStatus.VERIFIED);
            post.setApproveCount(0);
            post.setRejectCount(0);
            postRepository.save(post);

            int invalidatedReviews = postReviewRepository.invalidateValidReviewsByPostId(post.getId());

            log.info("Appeal {} accepted. Post {} marked VERIFIED and {} reviews invalidated",
                    appeal.getId(), post.getId(), invalidatedReviews);
        } else {
            log.info("Appeal {} rejected by admin", appeal.getId());
        }

        appeal = postAppealRepository.save(appeal);
        return toResponse(appeal);
    }

    private PostAppealEntity findAppealOrThrow(String appealId) {
        return postAppealRepository.findByIdWithPostAndUser(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại"));
    }

    private AppealResponse toResponse(PostAppealEntity appeal) {
        return AppealResponse.builder()
                .id(appeal.getId())
                .postId(appeal.getPost().getId())
                .userId(appeal.getUser().getId())
                .appealReason(appeal.getAppealReason())
                .evidenceUrls(appeal.getEvidenceUrls())
                .attemptNumber(appeal.getAttemptNumber())
                .status(appeal.getStatus())
                .adminNote(appeal.getAdminNote())
                .createdAt(appeal.getCreatedAt())
                .build();
    }

    private List<String> normalizeEvidenceUrls(List<String> evidenceUrls) {
        if (evidenceUrls == null) {
            return null;
        }

        List<String> sanitized = evidenceUrls.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));

        return sanitized.isEmpty() ? null : sanitized;
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
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
}
