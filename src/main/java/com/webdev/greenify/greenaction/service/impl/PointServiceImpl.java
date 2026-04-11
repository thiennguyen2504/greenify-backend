package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.dto.response.PointHistoryResponse;
import com.webdev.greenify.greenaction.dto.response.TotalPointsResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import com.webdev.greenify.greenaction.mapper.PointMapper;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.greenaction.service.PointService;
import com.webdev.greenify.point.entity.PointWalletEntity;
import com.webdev.greenify.point.repository.PointWalletRepository;
import com.webdev.greenify.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointServiceImpl implements PointService {

    private static final BigDecimal CTV_REVIEW_POINTS = new BigDecimal("5.00");
    private static final String CTV_REVIEW_ACTION_DESCRIPTION = "Duyệt bài hợp lệ với tư cách CTV";
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int POINT_EXPIRATION_MONTHS = 2;
        private static final BigDecimal ZERO_POINTS = BigDecimal.ZERO;
        private static final String DEFAULT_WALLET_STATUS = "ACTIVE";

    private final PointTransactionRepository pointTransactionRepository;
        private final PointWalletRepository pointWalletRepository;
    private final PointMapper pointMapper;

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

        increaseWalletPoints(user, points);
        transaction = pointTransactionRepository.save(transaction);

        log.info("Awarded {} points to user {} for post {}, expires at {}",
                points, user.getId(), post.getId(), expiresAt);

        return transaction;
    }

    @Override
    @Transactional
    public PointTransactionEntity awardPointsToReviewer(
            UserEntity reviewer,
            String reviewId,
            GreenActionPostEntity post) {

        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(POINT_EXPIRATION_MONTHS);

        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .user(reviewer)
                .points(CTV_REVIEW_POINTS)
                .actionDescription(CTV_REVIEW_ACTION_DESCRIPTION)
                .sourcePostId(post.getId())
                .sourceReviewId(reviewId)
                .expiresAt(expiresAt)
                .build();

        increaseWalletPoints(reviewer, CTV_REVIEW_POINTS);
        transaction = pointTransactionRepository.save(transaction);

        log.info("Awarded {} points to reviewer {} for reviewing post {}, expires at {}",
                CTV_REVIEW_POINTS, reviewer.getId(), post.getId(), expiresAt);

        return transaction;
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

        List<PointHistoryResponse> content = transactionsPage.getContent().stream()
                .map(pointMapper::toPointHistoryResponse)
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
            // Create negative transaction to deduct expired points
            BigDecimal deductionAmount = expiredTransaction.getPoints().negate();
            
            String deductionDescription = String.format(
                    "Điểm hết hạn: %s",
                    expiredTransaction.getActionDescription());

            PointTransactionEntity deduction = PointTransactionEntity.builder()
                    .user(expiredTransaction.getUser())
                    .points(deductionAmount)
                    .actionDescription(deductionDescription)
                    .expiredTransactionId(expiredTransaction.getId())
                    .build();

            decreaseWalletAvailablePoints(expiredTransaction.getUser(), expiredTransaction.getPoints());
            pointTransactionRepository.save(deduction);
            processedCount++;

            log.info("Expired {} points from user {} (transaction {})",
                    expiredTransaction.getPoints(),
                    expiredTransaction.getUser().getId(),
                    expiredTransaction.getId());
        }

        if (processedCount > 0) {
            log.info("Processed {} expired point transactions", processedCount);
        }
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

        private void increaseWalletPoints(UserEntity user, BigDecimal amount) {
                PointWalletEntity wallet = getOrCreateWalletForUpdate(user);
                wallet.setAvailablePoints(wallet.getAvailablePoints().add(amount));
                wallet.setTotalPoints(wallet.getTotalPoints().add(amount));
                pointWalletRepository.save(wallet);
        }

        private void decreaseWalletAvailablePoints(UserEntity user, BigDecimal amount) {
                PointWalletEntity wallet = getOrCreateWalletForUpdate(user);
                BigDecimal newAvailablePoints = wallet.getAvailablePoints().subtract(amount);
                wallet.setAvailablePoints(newAvailablePoints.max(ZERO_POINTS));
                pointWalletRepository.save(wallet);
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
                                .availablePoints(availablePoints)
                                .totalPoints(accumulatedPoints)
                                .weeklyPoints(ZERO_POINTS)
                                .status(DEFAULT_WALLET_STATUS)
                                .build();
                return pointWalletRepository.save(wallet);
        }
}
