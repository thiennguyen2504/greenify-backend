package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.file.enumeration.EventImageType;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.dto.response.PointHistoryResponse;
import com.webdev.greenify.greenaction.dto.response.TotalPointsResponse;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import com.webdev.greenify.greenaction.mapper.PointMapper;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.greenaction.service.PointService;
import com.webdev.greenify.leaderboard.service.LeaderboardService;
import com.webdev.greenify.point.entity.PointWalletEntity;
import com.webdev.greenify.point.repository.PointWalletRepository;
import com.webdev.greenify.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.webdev.greenify.notification.enumeration.NotificationType;
import com.webdev.greenify.notification.event.NotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointServiceImpl implements PointService {

    private static final BigDecimal DEFAULT_CTV_REVIEW_POINTS = new BigDecimal("1.00");
    private static final String CTV_REVIEW_ACTION_DESCRIPTION = "Duyệt bài hợp lệ với tư cách CTV";
    private static final List<String> REVIEWER_ACTION_TYPE_NAMES = Arrays.asList(
            "Duyệt bài hợp lệ với tư cách CTV",
            "Review posts as a Contributor");
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int POINT_EXPIRATION_MONTHS = 2;
    private static final String EVENT_SOURCE_PREFIX = "Sự Kiện: ";
    private static final String GREEN_ACTION_SOURCE_PREFIX = "Hành Động Xanh: ";
    private static final BigDecimal ZERO_POINTS = BigDecimal.ZERO;
    private static final String DEFAULT_WALLET_STATUS = "ACTIVE";

    private final PointTransactionRepository pointTransactionRepository;
    private final GreenActionPostRepository greenActionPostRepository;
    private final GreenActionTypeRepository greenActionTypeRepository;
    private final EventRepository eventRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointMapper pointMapper;
    private final LeaderboardService leaderboardService;
    private final ApplicationEventPublisher eventPublisher;

    private volatile BigDecimal reviewerPointsCache;

    @Override
    @Transactional
    public PointTransactionEntity awardPointsToPostAuthor(
            UserEntity user,
            GreenActionPostEntity post,
            BigDecimal points) {

        String actionDescription = String.format(
                "Hoàn thành hành động xanh: %s",
                post.getActionType().getActionName());

        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(POINT_EXPIRATION_MONTHS);

        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .user(user)
                .points(points)
                .actionDescription(actionDescription)
                .sourcePostId(post.getId())
                .expiresAt(expiresAt)
                .build();

        transaction = pointTransactionRepository.save(transaction);
        PointWalletEntity updatedWallet = increaseWalletPoints(user, points, transaction.getCreatedAt());
        leaderboardService.updateScore(user.getId(), updatedWallet.getWeeklyPoints(), updatedWallet.getLastPointEarnedAt());

        log.info("Awarded {} points to user {} for post {}, expires at {}",
                points, user.getId(), post.getId(), expiresAt);

        // Publish notification
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                user.getId(),
                "Nhận điểm thưởng",
                "Bạn vừa nhận được " + points + " điểm từ hành động: " + actionDescription,
                NotificationType.POINT_RECEIVED,
                post.getId()
        ));

        return transaction;
    }

    @Override
    @Transactional
    public PointTransactionEntity awardPointsToReviewer(
            UserEntity reviewer,
            String reviewId,
            GreenActionPostEntity post) {

        BigDecimal reviewerPoints = resolveReviewerPoints();
        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(POINT_EXPIRATION_MONTHS);

        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .user(reviewer)
                .points(reviewerPoints)
                .actionDescription(CTV_REVIEW_ACTION_DESCRIPTION)
                .sourcePostId(post.getId())
                .sourceReviewId(reviewId)
                .expiresAt(expiresAt)
                .build();

        transaction = pointTransactionRepository.save(transaction);
        PointWalletEntity updatedWallet = increaseWalletPoints(reviewer, reviewerPoints, transaction.getCreatedAt());
        leaderboardService.updateScore(
                reviewer.getId(),
                updatedWallet.getWeeklyPoints(),
                updatedWallet.getLastPointEarnedAt());

        log.info("Awarded {} points to reviewer {} for reviewing post {}, expires at {}",
                reviewerPoints, reviewer.getId(), post.getId(), expiresAt);

        // Publish notification
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                reviewer.getId(),
                "Nhận điểm thưởng",
                "Bạn vừa nhận được " + reviewerPoints + " điểm từ việc duyệt bài viết.",
                NotificationType.POINT_RECEIVED,
                post.getId()
        ));

        return transaction;
    }

    @Override
    @Transactional
    public PointTransactionEntity awardPointsForEventParticipation(
            UserEntity user,
            EventEntity event) {

        BigDecimal points = BigDecimal.valueOf(event.getRewardPoints());
        String actionDescription = String.format(
                "Tham gia sự kiện: %s",
                event.getTitle());

        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(POINT_EXPIRATION_MONTHS);

        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .user(user)
                .points(points)
                .actionDescription(actionDescription)
                .sourcePostId(event.getId())
                .expiresAt(expiresAt)
                .build();

        transaction = pointTransactionRepository.save(transaction);
        PointWalletEntity updatedWallet = increaseWalletPoints(user, points, transaction.getCreatedAt());
        leaderboardService.updateScore(user.getId(), updatedWallet.getWeeklyPoints(), updatedWallet.getLastPointEarnedAt());

        log.info("Awarded {} points to user {} for event participation {}, expires at {}",
                points, user.getId(), event.getId(), expiresAt);

        // Publish notification
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                user.getId(),
                "Nhận điểm thưởng",
                "Bạn vừa nhận được " + points + " điểm từ việc tham gia sự kiện: " + event.getTitle(),
                NotificationType.POINT_RECEIVED,
                event.getId()
        ));

        return transaction;
    }

    private BigDecimal resolveReviewerPoints() {
        BigDecimal cachedPoints = reviewerPointsCache;
        if (cachedPoints != null) {
            return cachedPoints;
        }

        synchronized (this) {
            if (reviewerPointsCache == null) {
                reviewerPointsCache = REVIEWER_ACTION_TYPE_NAMES.stream()
                        .map(greenActionTypeRepository::findFirstByActionNameIgnoreCaseAndIsActiveTrue)
                        .flatMap(Optional::stream)
                        .map(GreenActionTypeEntity::getSuggestedPoints)
                        .filter(points -> points != null && points.compareTo(BigDecimal.ZERO) > 0)
                        .findFirst()
                        .orElse(DEFAULT_CTV_REVIEW_POINTS);
            }
            return reviewerPointsCache;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TotalPointsResponse getTotalPointsForCurrentUser() {
        String userId = getCurrentUserId();

        PointWalletEntity wallet = pointWalletRepository.findByUserId(userId)
                .orElse(null);

        BigDecimal accumulatedPoints = wallet != null
                ? wallet.getTotalPoints()
                : pointTransactionRepository.sumAccumulatedPointsByUserId(userId);
        BigDecimal availablePoints = wallet != null
                ? wallet.getAvailablePoints()
                : pointTransactionRepository.sumAvailablePointsByUserId(userId);
        long transactionCount = pointTransactionRepository.countByUserId(userId);

        return TotalPointsResponse.builder()
                .userId(userId)
                .accumulatedPoints(accumulatedPoints)
                .availablePoints(availablePoints)
                .transactionCount(transactionCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PointHistoryResponse> getPointHistoryForCurrentUser(int page, int size) {
        String userId = getCurrentUserId();

        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize);

        Page<PointTransactionEntity> transactionsPage = pointTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PointTransactionEntity> transactions = transactionsPage.getContent();
        Set<String> sourceIds = extractSourceIds(transactions);
        Map<String, GreenActionPostEntity> postsById = loadPostsById(sourceIds);
        Map<String, EventEntity> eventsById = loadEventsById(sourceIds);

        List<PointHistoryResponse> content = transactions.stream()
                .map(transaction -> toPointHistoryResponse(transaction, postsById, eventsById))
                .toList();

        return PagedResponse.of(
                content,
                transactionsPage.getNumber(),
                transactionsPage.getSize(),
                transactionsPage.getTotalElements(),
                transactionsPage.getTotalPages());
    }

    @Override
    @Transactional
    public void processExpiredPoints() {
        LocalDateTime now = LocalDateTime.now();
        List<PointTransactionEntity> expiredTransactions =
                pointTransactionRepository.findExpiredPointsNotProcessed(now);

        int processedCount = 0;
        for (PointTransactionEntity expiredTransaction : expiredTransactions) {
            BigDecimal deductedPoints = decreaseWalletAvailablePoints(
                    expiredTransaction.getUser(),
                    expiredTransaction.getPoints());

            if (deductedPoints.compareTo(ZERO_POINTS) <= 0) {
                log.info("Skipped expiring points for user {} (transaction {}) because no available points remain",
                        expiredTransaction.getUser().getId(),
                        expiredTransaction.getId());
                continue;
            }

            String deductionDescription = String.format(
                    "Điểm hết hạn: %s",
                    expiredTransaction.getActionDescription());

            PointTransactionEntity deduction = PointTransactionEntity.builder()
                    .user(expiredTransaction.getUser())
                    .points(deductedPoints.negate())
                    .actionDescription(deductionDescription)
                    .expiredTransactionId(expiredTransaction.getId())
                    .build();

            pointTransactionRepository.save(deduction);
            processedCount++;

            log.info("Expired {} points from user {} (transaction {})",
                    deductedPoints,
                    expiredTransaction.getUser().getId(),
                    expiredTransaction.getId());
        }

        if (processedCount > 0) {
            log.info("Processed {} expired point transactions", processedCount);
        }
    }

    @Override
    public void invalidateActionTypeCache() {
        // reviewerPointsCache is lazily resolved from action type definitions.
        reviewerPointsCache = null;
    }

    private PointHistoryResponse toPointHistoryResponse(
            PointTransactionEntity transaction,
            Map<String, GreenActionPostEntity> postsById,
            Map<String, EventEntity> eventsById) {

        PointHistoryResponse response = pointMapper.toPointHistoryResponse(transaction);
        SourceMetadata sourceMetadata = resolveSourceMetadata(transaction, postsById, eventsById);
        response.setSourceName(sourceMetadata.sourceName());
        response.setSourceDisplayUrl(sourceMetadata.sourceDisplayUrl());
        return response;
    }

    private SourceMetadata resolveSourceMetadata(
            PointTransactionEntity transaction,
            Map<String, GreenActionPostEntity> postsById,
            Map<String, EventEntity> eventsById) {

        String sourceId = transaction.getSourcePostId();
        if (!hasText(sourceId)) {
            return new SourceMetadata(transaction.getActionDescription(), null);
        }

        GreenActionPostEntity post = postsById.get(sourceId);
        if (post != null) {
            return new SourceMetadata(resolveGreenActionSourceName(post), resolveGreenActionDisplayUrl(post));
        }

        EventEntity event = eventsById.get(sourceId);
        if (event != null) {
            return new SourceMetadata(resolveEventSourceName(event), resolveEventDisplayUrl(event));
        }

        return new SourceMetadata(transaction.getActionDescription(), null);
    }

    private String resolveGreenActionSourceName(GreenActionPostEntity post) {
        if (post.getActionType() == null || !hasText(post.getActionType().getActionName())) {
            return "Hành Động Xanh";
        }
        return GREEN_ACTION_SOURCE_PREFIX + post.getActionType().getActionName();
    }

    private String resolveGreenActionDisplayUrl(GreenActionPostEntity post) {
        if (post.getPostImage() == null) {
            return null;
        }
        return firstNonBlank(post.getPostImage().getImageUrl());
    }

    private String resolveEventSourceName(EventEntity event) {
        if (!hasText(event.getTitle())) {
            return "Sự Kiện";
        }
        return EVENT_SOURCE_PREFIX + event.getTitle();
    }

    private String resolveEventDisplayUrl(EventEntity event) {
        if (event.getImages() == null || event.getImages().isEmpty()) {
            return null;
        }

        String thumbnailUrl = event.getImages().stream()
                .filter(image -> image != null && image.getImageType() == EventImageType.THUMBNAIL)
                .map(image -> firstNonBlank(image.getImageUrl()))
                .filter(this::hasText)
                .findFirst()
                .orElse(null);

        if (hasText(thumbnailUrl)) {
            return thumbnailUrl;
        }

        return event.getImages().stream()
                .map(image -> image == null ? null : firstNonBlank(image.getImageUrl()))
                .filter(this::hasText)
                .findFirst()
                .orElse(null);
    }

    private Set<String> extractSourceIds(List<PointTransactionEntity> transactions) {
        return transactions.stream()
                .map(PointTransactionEntity::getSourcePostId)
                .map(this::firstNonBlank)
                .filter(this::hasText)
                .collect(Collectors.toSet());
    }

    private Map<String, GreenActionPostEntity> loadPostsById(Set<String> sourceIds) {
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        return greenActionPostRepository.findByIdIn(sourceIds).stream()
                .collect(Collectors.toMap(GreenActionPostEntity::getId, Function.identity()));
    }

    private Map<String, EventEntity> loadEventsById(Set<String> sourceIds) {
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        return eventRepository.findByIdIn(sourceIds).stream()
                .collect(Collectors.toMap(EventEntity::getId, Function.identity()));
    }

    private String firstNonBlank(String value) {
        return hasText(value) ? value : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SourceMetadata(String sourceName, String sourceDisplayUrl) {
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private PointWalletEntity increaseWalletPoints(UserEntity user, BigDecimal amount, LocalDateTime pointEarnedAt) {
        PointWalletEntity wallet = getOrCreateWalletForUpdate(user);
        BigDecimal currentWeeklyPoints = wallet.getWeeklyPoints() != null ? wallet.getWeeklyPoints() : ZERO_POINTS;

        wallet.setAvailablePoints(wallet.getAvailablePoints().add(amount));
        wallet.setTotalPoints(wallet.getTotalPoints().add(amount));
        wallet.setWeeklyPoints(currentWeeklyPoints.add(amount));
        wallet.setLastPointEarnedAt(pointEarnedAt != null ? pointEarnedAt : LocalDateTime.now());

        return pointWalletRepository.save(wallet);
    }

    private BigDecimal decreaseWalletAvailablePoints(UserEntity user, BigDecimal amount) {
        PointWalletEntity wallet = getOrCreateWalletForUpdate(user);
        BigDecimal currentAvailablePoints = wallet.getAvailablePoints().max(ZERO_POINTS);
        BigDecimal deductedPoints = amount.min(currentAvailablePoints);
        BigDecimal newAvailablePoints = currentAvailablePoints.subtract(deductedPoints);
        wallet.setAvailablePoints(newAvailablePoints);
        pointWalletRepository.save(wallet);
        return deductedPoints;
    }

    private PointWalletEntity getOrCreateWalletForUpdate(UserEntity user) {
        return pointWalletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> bootstrapWallet(user));
    }

    private PointWalletEntity bootstrapWallet(UserEntity user) {
        BigDecimal accumulatedPoints = pointTransactionRepository.sumAccumulatedPointsByUserId(user.getId());
        BigDecimal availablePoints = pointTransactionRepository.sumAvailablePointsByUserId(user.getId());

        PointWalletEntity wallet = PointWalletEntity.builder()
                .user(user)
                .availablePoints(availablePoints.max(ZERO_POINTS))
                .totalPoints(accumulatedPoints)
                .weeklyPoints(ZERO_POINTS)
                .status(DEFAULT_WALLET_STATUS)
                .build();

        try {
            return pointWalletRepository.save(wallet);
        } catch (DataIntegrityViolationException ex) {
            return pointWalletRepository.findByUserIdForUpdate(user.getId())
                    .orElseThrow(() -> ex);
        }
    }
}
