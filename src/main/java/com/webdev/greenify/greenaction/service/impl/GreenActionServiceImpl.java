package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.file.entity.PostImageEntity;
import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.greenaction.constant.ActionTypeConstants;
import com.webdev.greenify.greenaction.dto.request.CreateActionTypeRequest;
import com.webdev.greenify.greenaction.dto.request.CreateGreenActionPostRequest;
import com.webdev.greenify.greenaction.dto.request.UpdateActionTypeRequest;
import com.webdev.greenify.greenaction.dto.response.GreenActionTypeResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostDetailResponse;
import com.webdev.greenify.greenaction.dto.response.PostReviewResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostSummaryResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.entity.PostReviewEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.mapper.GreenActionMapper;
import com.webdev.greenify.greenaction.mapper.ReviewMapper;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
import com.webdev.greenify.greenaction.repository.PostReviewRepository;
import com.webdev.greenify.greenaction.service.GreenActionService;
import com.webdev.greenify.greenaction.service.LocationSnapshotService;
import com.webdev.greenify.greenaction.service.PointService;
import com.webdev.greenify.greenaction.specification.GreenActionPostSpecification;
import com.webdev.greenify.trashspot.service.TrashSpotService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GreenActionServiceImpl implements GreenActionService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final GreenActionPostRepository postRepository;
    private final GreenActionTypeRepository actionTypeRepository;
    private final PostReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GreenActionMapper greenActionMapper;
    private final ReviewMapper reviewMapper;
    private final ImageMapper imageMapper;
    private final LocationSnapshotService locationSnapshotService;
    private final PointService pointService;
    private final TrashSpotService trashSpotService;

    @Override
    @Transactional
    public GreenActionPostDetailResponse createPost(CreateGreenActionPostRequest request) {
        String currentUserId = getCurrentUserId();
        
        // Validate action type exists and is active
        GreenActionTypeEntity actionType = actionTypeRepository.findByIdAndIsActiveTrue(request.getActionTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hành động hoặc loại hành động đã ngừng hoạt động"));

        // Validate location if required
        if (Boolean.TRUE.equals(actionType.getLocationRequired())) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new AppException("Vị trí là bắt buộc cho loại hành động này", HttpStatus.BAD_REQUEST);
            }
        }

        // Get current user
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Use current date if actionDate is not provided
        LocalDate effectiveActionDate = request.getActionDate() != null 
                ? request.getActionDate() 
                : LocalDate.now();

        // Create post entity
        GreenActionPostEntity post = GreenActionPostEntity.builder()
                .user(user)
                .actionType(actionType)
                .caption(request.getCaption())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .location(locationSnapshotService.resolveLocationSnapshot(request.getLatitude(), request.getLongitude()))
                .actionDate(effectiveActionDate)
                .status(PostStatus.PENDING_REVIEW)
                .approveCount(0)
                .rejectCount(0)
                .build();

        // Create and attach post image
        if (request.getMedia() != null) {
            PostImageEntity postImage = imageMapper.toPostImageEntity(request.getMedia());
            postImage.setPost(post);
            post.setPostImage(postImage);
        }

        post = postRepository.save(post);
        log.info("Green action post created with ID: {} by user: {}", post.getId(), currentUserId);

        GreenActionPostDetailResponse response = greenActionMapper.toDetailResponse(post);
        response.setReviews(List.of());
        return response;
    }

    @Override
    @Transactional
    public GreenActionTypeResponse createActionType(CreateActionTypeRequest request) {
        String adminId = getCurrentUserId();

        if (request.getSuggestedPoints() == null || request.getSuggestedPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Điểm gợi ý phải lớn hơn 0", HttpStatus.BAD_REQUEST);
        }

        String normalizedActionName = normalizeRequiredText(request.getActionName(), "Action name");

        actionTypeRepository.findFirstByActionNameIgnoreCaseAndIsActiveTrue(normalizedActionName)
                .ifPresent(existing -> {
                    throw new AppException("Loại hành động với tên này đã tồn tại", HttpStatus.CONFLICT);
                });

        GreenActionTypeEntity actionType = GreenActionTypeEntity.builder()
                .groupName(normalizeRequiredText(request.getGroupName(), "Group name"))
                .actionName(normalizedActionName)
                .suggestedPoints(request.getSuggestedPoints())
                .locationRequired(request.getLocationRequired())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        actionType = actionTypeRepository.save(actionType);

        if (isCacheSensitiveActionName(actionType.getActionName())) {
            invalidateActionTypeCaches(actionType.getActionName());
        }

        log.info("Admin {} created action type {} with actionName={} groupName={} isActive={}",
                adminId,
                actionType.getId(),
                actionType.getActionName(),
                actionType.getGroupName(),
                actionType.getIsActive());

        return toActionTypeResponse(actionType);
    }

    @Override
    @Transactional
    public GreenActionTypeResponse updateActionType(String actionTypeId, UpdateActionTypeRequest request) {
        String adminId = getCurrentUserId();

        GreenActionTypeEntity actionType = actionTypeRepository.findById(actionTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hành động"));

        String oldActionName = actionType.getActionName();
        List<String> changedFields = new ArrayList<>();

        if (request.getGroupName() != null) {
            actionType.setGroupName(normalizeRequiredText(request.getGroupName(), "Group name"));
            changedFields.add("groupName");
        }

        if (request.getActionName() != null) {
            actionType.setActionName(normalizeRequiredText(request.getActionName(), "Action name"));
            changedFields.add("actionName");
        }

        if (request.getSuggestedPoints() != null) {
            if (request.getSuggestedPoints().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException("Điểm gợi ý phải lớn hơn 0", HttpStatus.BAD_REQUEST);
            }
            actionType.setSuggestedPoints(request.getSuggestedPoints());
            changedFields.add("suggestedPoints");
        }

        if (request.getLocationRequired() != null) {
            actionType.setLocationRequired(request.getLocationRequired());
            changedFields.add("locationRequired");
        }

        if (request.getIsActive() != null) {
            actionType.setIsActive(request.getIsActive());
            changedFields.add("isActive");
        }

        boolean shouldCheckUniqueByName = request.getActionName() != null || Boolean.TRUE.equals(actionType.getIsActive());
        if (shouldCheckUniqueByName) {
            String currentActionTypeId = actionType.getId();
            actionTypeRepository.findFirstByActionNameIgnoreCaseAndIsActiveTrue(actionType.getActionName())
                    .filter(existing -> !existing.getId().equals(currentActionTypeId))
                    .ifPresent(existing -> {
                        throw new AppException("Loại hành động với tên này đã tồn tại", HttpStatus.CONFLICT);
                    });
        }

        actionType = actionTypeRepository.save(actionType);

        if (requiresActionTypeCacheInvalidation(oldActionName, actionType.getActionName())) {
            invalidateActionTypeCaches(actionType.getActionName());
        }

        log.info("Admin {} updated action type {} fields={} actionName={} isActive={}",
                adminId,
                actionType.getId(),
                changedFields,
                actionType.getActionName(),
                actionType.getIsActive());

        return toActionTypeResponse(actionType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GreenActionTypeResponse> getAllActionTypes() {
        return actionTypeRepository.findAllByOrderByGroupNameAscActionNameAsc().stream()
                .map(this::toActionTypeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GreenActionPostSummaryResponse> getTopPosts(int limit) {
        int effectiveLimit = clampPageSize(limit);
        Pageable pageable = PageRequest.of(0, effectiveLimit);
        
        List<GreenActionPostEntity> posts = postRepository.findTopPostsWithUserAndActionType(
                PostStatus.VERIFIED, pageable);

        Map<String, List<PostReviewResponse>> reviewsByPostId = getReviewsGroupedByPostIds(
                posts.stream().map(GreenActionPostEntity::getId).toList());

        return posts.stream()
                .map(post -> {
                    GreenActionPostSummaryResponse response = greenActionMapper.toSummaryResponse(post);
                    response.setReviews(reviewsByPostId.getOrDefault(post.getId(), List.of()));
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GreenActionPostSummaryResponse> getPostsByFilter(
            PostStatus status,
            String actionTypeId,
            String groupName,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            String sortDirection,
            int page,
            int size) {
        
        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);
        Sort.Direction createdAtDirection = resolveCreatedAtDirection(sortDirection);
        
        Pageable pageable = PageRequest.of(effectivePage, effectiveSize,
            Sort.by(createdAtDirection, "createdAt"));

        Specification<GreenActionPostEntity> spec = resolveStatusSpecification(status)
            .and(GreenActionPostSpecification.hasActionTypeId(actionTypeId))
            .and(GreenActionPostSpecification.hasGroupNameLike(groupName))
            .and(GreenActionPostSpecification.hasAuthorEmailOrDisplayNameLike(search))
            .and(GreenActionPostSpecification.actionDateFrom(fromDate))
            .and(GreenActionPostSpecification.actionDateTo(toDate));

        Page<GreenActionPostEntity> postsPage = postRepository.findAll(spec, pageable);

        Map<String, List<PostReviewResponse>> reviewsByPostId = getReviewsGroupedByPostIds(
                postsPage.getContent().stream().map(GreenActionPostEntity::getId).toList());

        List<GreenActionPostSummaryResponse> content = postsPage.getContent().stream()
                .map(post -> {
                    GreenActionPostSummaryResponse response = greenActionMapper.toSummaryResponse(post);
                    response.setReviews(reviewsByPostId.getOrDefault(post.getId(), List.of()));
                    return response;
                })
                .toList();

        return PagedResponse.of(
                content,
                postsPage.getNumber(),
                postsPage.getSize(),
                postsPage.getTotalElements(),
                postsPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GreenActionPostSummaryResponse> getPostHistoryForCurrentUser(
            PostStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        String currentUserId = getCurrentUserId();

        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(
                effectivePage,
                effectiveSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<GreenActionPostEntity> spec = GreenActionPostSpecification.hasUserId(currentUserId)
                .and(GreenActionPostSpecification.buildSpecification(status, null, null, fromDate, toDate));

        Page<GreenActionPostEntity> postsPage = postRepository.findAll(spec, pageable);

        Map<String, List<PostReviewResponse>> reviewsByPostId = getReviewsGroupedByPostIds(
                postsPage.getContent().stream().map(GreenActionPostEntity::getId).toList());

        List<GreenActionPostSummaryResponse> content = postsPage.getContent().stream()
                .map(post -> {
                    GreenActionPostSummaryResponse response = greenActionMapper.toSummaryResponse(post);
                    response.setReviews(reviewsByPostId.getOrDefault(post.getId(), List.of()));
                    return response;
                })
                .toList();

        return PagedResponse.of(
                content,
                postsPage.getNumber(),
                postsPage.getSize(),
                postsPage.getTotalElements(),
                postsPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public GreenActionPostDetailResponse getPostDetail(String postId) {
        String currentUserId = getCurrentUserId();
        
        GreenActionPostEntity post = postRepository.findByIdWithUserAndActionType(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết"));

        boolean isOwner = post.getUser().getId().equals(currentUserId);
        boolean canViewAllPosts = hasCurrentRole("ROLE_CTV") || hasCurrentRole("ROLE_ADMIN");

        if (!isOwner && !canViewAllPosts && post.getStatus() != PostStatus.VERIFIED) {
            throw new AppException("Bài viết không khả dụng", HttpStatus.FORBIDDEN);
        }

        GreenActionPostDetailResponse response = greenActionMapper.toDetailResponse(post);
        response.setReviews(reviewMapper.toPostReviewResponseList(
                reviewRepository.findByPostIdAndIsValidTrueOrderByCreatedAtDesc(post.getId())));
        return response;
    }

    private Map<String, List<PostReviewResponse>> getReviewsGroupedByPostIds(List<String> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<PostReviewEntity> reviews = reviewRepository.findByPostIdsAndIsValidTrue(postIds);
        return reviews.stream().collect(Collectors.groupingBy(
                review -> review.getPost().getId(),
                LinkedHashMap::new,
                Collectors.mapping(reviewMapper::toPostReviewResponse, Collectors.toList())));
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private Specification<GreenActionPostEntity> resolveStatusSpecification(PostStatus requestedStatus) {
        if (requestedStatus != null) {
            if (isPublicFeedRestrictedRole() && requestedStatus != PostStatus.VERIFIED) {
                throw new AppException("Bạn không có quyền xem trạng thái bài viết này", HttpStatus.FORBIDDEN);
            }
            return GreenActionPostSpecification.hasStatus(requestedStatus);
        }

        if (hasCurrentRole("ROLE_ADMIN")) {
            return (root, query, cb) -> cb.conjunction();
        }

        if (hasCurrentRole("ROLE_CTV")) {
            return GreenActionPostSpecification.hasStatuses(List.of(PostStatus.VERIFIED, PostStatus.PENDING_REVIEW));
        }

        return GreenActionPostSpecification.hasStatus(PostStatus.VERIFIED);
    }

    private Sort.Direction resolveCreatedAtDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return Sort.Direction.DESC;
        }

        String normalized = sortDirection.trim();
        if ("asc".equalsIgnoreCase(normalized)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(normalized)) {
            return Sort.Direction.DESC;
        }

        throw new AppException("sortDirection phải là asc hoặc desc", HttpStatus.BAD_REQUEST);
    }

    private boolean isPublicFeedRestrictedRole() {
        return !hasCurrentRole("ROLE_CTV") && !hasCurrentRole("ROLE_ADMIN");
    }

    private boolean hasCurrentRole(String roleAuthority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleAuthority::equals);
    }

    private boolean requiresActionTypeCacheInvalidation(String previousActionName, String currentActionName) {
        return isCacheSensitiveActionName(previousActionName) || isCacheSensitiveActionName(currentActionName);
    }

    private boolean isCacheSensitiveActionName(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            return false;
        }

        return ActionTypeConstants.REPORTER_ACTION_NAMES.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(actionName))
                || ActionTypeConstants.REVIEWER_ACTION_NAMES.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(actionName));
    }

    private void invalidateActionTypeCaches(String actionName) {
        // These caches are lazily loaded from green_action_types in PointServiceImpl/TrashSpotServiceImpl.
        pointService.invalidateActionTypeCache();
        trashSpotService.invalidateActionTypeCache();

        log.info("Invalidated action-type caches after admin changed cache-sensitive action types for action name {}", actionName);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AppException(fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private GreenActionTypeResponse toActionTypeResponse(GreenActionTypeEntity actionType) {
        return GreenActionTypeResponse.builder()
                .id(actionType.getId())
                .groupName(actionType.getGroupName())
                .actionName(actionType.getActionName())
                .suggestedPoints(actionType.getSuggestedPoints())
                .locationRequired(actionType.getLocationRequired())
                .isActive(actionType.getIsActive())
                .build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
