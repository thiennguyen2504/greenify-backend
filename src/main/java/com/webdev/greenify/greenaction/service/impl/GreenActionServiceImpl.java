package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.dto.request.CreateGreenActionPostRequest;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostDetailResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostSummaryResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.mapper.GreenActionMapper;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GreenActionServiceImpl implements GreenActionService {

    private final GreenActionPostRepository postRepository;
    private final GreenActionTypeRepository actionTypeRepository;
    private final UserRepository userRepository;
    private final GreenActionMapper greenActionMapper;

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
                .mediaUrl(request.getMediaUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .actionDate(effectiveActionDate)
                .status(PostStatus.PENDING_REVIEW)
                .approveCount(0)
                .rejectCount(0)
                .build();

        post = postRepository.save(post);
        log.info("Green action post created with ID: {} by user: {}", post.getId(), currentUserId);

        return greenActionMapper.toDetailResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GreenActionPostSummaryResponse> getTopPosts(int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, effectiveLimit);
        
        List<GreenActionPostEntity> posts = postRepository.findTopPostsWithUserAndActionType(
                PostStatus.VERIFIED, pageable);
        
        return posts.stream()
                .map(greenActionMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GreenActionPostSummaryResponse> getPostsByFilter(
            String actionTypeId,
            String groupName,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "approveCount")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));

        Specification<GreenActionPostEntity> spec = GreenActionPostSpecification.buildSpecification(
                PostStatus.VERIFIED, actionTypeId, groupName, fromDate, toDate);

        Page<GreenActionPostEntity> postsPage = postRepository.findAll(spec, pageable);

        List<GreenActionPostSummaryResponse> content = postsPage.getContent().stream()
                .map(greenActionMapper::toSummaryResponse)
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

        return greenActionMapper.toDetailResponse(post);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
