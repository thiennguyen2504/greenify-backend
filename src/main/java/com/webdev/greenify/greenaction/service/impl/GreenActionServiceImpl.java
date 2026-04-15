package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.file.entity.PostImageEntity;
import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.greenaction.dto.request.CreateGreenActionPostRequest;
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
import com.webdev.greenify.greenaction.specification.GreenActionPostSpecification;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Override
    @Transactional
    public GreenActionPostDetailResponse createPost(CreateGreenActionPostRequest request) {
        String currentUserId = getCurrentUserId();
        
        // Validate action type exists and is active
        GreenActionTypeEntity actionType = actionTypeRepository.findByIdAndIsActiveTrue(request.getActionTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Action type not found or inactive"));

        // Validate location if required
        if (Boolean.TRUE.equals(actionType.getLocationRequired())) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new AppException("Vị trí là bắt buộc cho loại hành động này", HttpStatus.BAD_REQUEST);
            }
        }

        // Get current user
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
        response.setLocation(buildMockLocation(post.getLatitude(), post.getLongitude()));
        response.setReviews(List.of());
        return response;
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
                    response.setLocation(buildMockLocation(post.getLatitude(), post.getLongitude()));
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
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        
        // Validate and clamp page/size
        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);
        
        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, 
                Sort.by(Sort.Direction.DESC, "approveCount")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));

        PostStatus effectiveStatus = status != null ? status : PostStatus.VERIFIED;

        Specification<GreenActionPostEntity> spec = GreenActionPostSpecification.buildSpecification(
            effectiveStatus, actionTypeId, groupName, fromDate, toDate);

        Page<GreenActionPostEntity> postsPage = postRepository.findAll(spec, pageable);

        Map<String, List<PostReviewResponse>> reviewsByPostId = getReviewsGroupedByPostIds(
                postsPage.getContent().stream().map(GreenActionPostEntity::getId).toList());

        List<GreenActionPostSummaryResponse> content = postsPage.getContent().stream()
                .map(post -> {
                    GreenActionPostSummaryResponse response = greenActionMapper.toSummaryResponse(post);
                    response.setLocation(buildMockLocation(post.getLatitude(), post.getLongitude()));
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
                    response.setLocation(buildMockLocation(post.getLatitude(), post.getLongitude()));
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
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // Check if user is the owner
        boolean isOwner = post.getUser().getId().equals(currentUserId);

        // If not owner, check if post is VERIFIED
        if (!isOwner && post.getStatus() != PostStatus.VERIFIED) {
            throw new AppException("Bài viết không khả dụng", HttpStatus.FORBIDDEN);
        }

        GreenActionPostDetailResponse response = greenActionMapper.toDetailResponse(post);
        response.setLocation(buildMockLocation(post.getLatitude(), post.getLongitude()));
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

    private String buildMockLocation(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return "Vi tri mo phong: chua co toa do";
        }
        return "tes ("
                + latitude.stripTrailingZeros().toPlainString()
                + ", "
                + longitude.stripTrailingZeros().toPlainString()
                + ")";
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
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
