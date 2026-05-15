package com.webdev.greenify.common.util;

import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import com.webdev.greenify.greenaction.entity.PostReviewEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.enumeration.ReviewDecision;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.greenaction.repository.PostReviewRepository;
import com.webdev.greenify.point.entity.PointLedgerEntity;
import com.webdev.greenify.point.enumeration.PointLedgerSourceType;
import com.webdev.greenify.point.enumeration.PointLedgerStatus;
import com.webdev.greenify.point.repository.PointLedgerRepository;
import com.webdev.greenify.point.entity.PointWalletEntity;
import com.webdev.greenify.point.repository.PointWalletRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncPostDataSeed {

    private final GreenActionPostRepository postRepository;
    private final PostReviewRepository reviewRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final UserRepository userRepository;

    @Transactional
    public void seed() {
        log.info("Starting SyncPostDataSeed to sync reviews and points for existing posts...");
        List<GreenActionPostEntity> allPosts = postRepository.findAll();
        List<UserEntity> ctvUsers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "CTV".equalsIgnoreCase(r.getName())))
                .toList();

        if (ctvUsers.isEmpty()) {
            log.warn("No CTV users found to create reviews. Aborting sync.");
            return;
        }

        int syncedPosts = 0;
        for (GreenActionPostEntity post : allPosts) {
            boolean hasReview = reviewRepository.existsByPost_Id(post.getId());
            if (!hasReview) {
                int approveCount = post.getApproveCount() != null ? post.getApproveCount() : 0;
                int rejectCount = post.getRejectCount() != null ? post.getRejectCount() : 0;
                
                // Seed Reviews
                for (int i = 0; i < approveCount; i++) {
                    UserEntity ctv = ctvUsers.get(ThreadLocalRandom.current().nextInt(ctvUsers.size()));
                    saveReview(post, ctv, ReviewDecision.APPROVE, null);
                }
                
                for (int i = 0; i < rejectCount; i++) {
                    UserEntity ctv = ctvUsers.get(ThreadLocalRandom.current().nextInt(ctvUsers.size()));
                    saveReview(post, ctv, ReviewDecision.REJECT, randomRejectReason());
                }
            }

            // Seed Point Transactions and Point Ledger for VERIFIED posts
            if (PostStatus.VERIFIED.equals(post.getStatus()) && post.getUser() != null) {
                BigDecimal points = post.getActionType() != null && post.getActionType().getSuggestedPoints() != null
                        ? post.getActionType().getSuggestedPoints()
                        : BigDecimal.ONE;

                boolean hasPointTx = pointTransactionRepository.existsBySourcePostIdAndUser_Id(post.getId(), post.getUser().getId());
                if (!hasPointTx) {
                    LocalDateTime earnedAt = post.getActionDate() != null ? post.getActionDate().atStartOfDay().plusHours(12) : LocalDateTime.now();
                    PointTransactionEntity tx = PointTransactionEntity.builder()
                            .user(post.getUser())
                            .points(points)
                            .actionDescription("Hoàn thành hành động xanh: " + (post.getActionType() != null ? post.getActionType().getActionName() : "Khác"))
                            .sourcePostId(post.getId())
                            .expiresAt(earnedAt.plusMonths(2))
                            .build();
                    tx.setCreatedAt(earnedAt);
                    pointTransactionRepository.save(tx);
                }

                boolean hasLedger = pointLedgerRepository.existsBySourceIdAndSourceType(post.getId(), PointLedgerSourceType.ACTION_POST);
                if (!hasLedger) {
                    PointLedgerEntity ledger = PointLedgerEntity.builder()
                            .user(post.getUser())
                            .amount(points)
                            .sourceType(PointLedgerSourceType.ACTION_POST)
                            .sourceId(post.getId())
                            .status(PointLedgerStatus.REWARDED)
                            .build();
                    pointLedgerRepository.save(ledger);
                }
            }

            syncedPosts++;
        }

        // Sync Point Wallet for all users
        List<UserEntity> allUsers = userRepository.findAll();
        for (UserEntity user : allUsers) {
            synchronizeWallet(user);
        }

        log.info("Finished SyncPostDataSeed. Synced {} posts and updated wallets.", syncedPosts);
    }

    private void saveReview(GreenActionPostEntity post, UserEntity reviewer, ReviewDecision decision, String rejectReason) {
        PostReviewEntity review = PostReviewEntity.builder()
                .post(post)
                .reviewer(reviewer)
                .decision(decision)
                .rejectReason(rejectReason)
                .isValid(true)
                .build();
        reviewRepository.save(review);
    }

    private String randomRejectReason() {
        List<String> reasons = List.of(
                "Bài viết có ảnh không rõ ràng, mờ nhòe.",
                "Ảnh không thể hiện rõ hành động xanh như mô tả.",
                "Nội dung và hình ảnh không khớp với nhau.",
                "Nghi ngờ ảnh lấy từ nguồn khác trên internet.",
                "Thiếu thông tin chứng minh để xác thực hoạt động."
        );
        return reasons.get(ThreadLocalRandom.current().nextInt(reasons.size()));
    }

    private void synchronizeWallet(UserEntity user) {
        BigDecimal availablePoints = pointTransactionRepository.sumAvailablePointsByUserId(user.getId());
        BigDecimal totalPoints = pointTransactionRepository.sumAccumulatedPointsByUserId(user.getId());

        PointWalletEntity wallet = pointWalletRepository.findByUserId(user.getId())
                .orElseGet(() -> PointWalletEntity.builder().user(user).build());

        wallet.setUser(user);
        wallet.setAvailablePoints(availablePoints != null ? availablePoints : BigDecimal.ZERO);
        wallet.setTotalPoints(totalPoints != null ? totalPoints : BigDecimal.ZERO);
        wallet.setWeeklyPoints(calculateWeeklyPoints(user.getId()));
        wallet.setLastPointEarnedAt(findLastPointEarnedAt(user.getId()));
        
        if (wallet.getStatus() == null || wallet.getStatus().isBlank()) {
            wallet.setStatus("ACTIVE");
        }

        pointWalletRepository.save(wallet);
    }

    private BigDecimal calculateWeeklyPoints(String userId) {
        LocalDateTime weekStart = LocalDateTime.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).withHour(0).withMinute(0).withSecond(0);

        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500))
                .stream()
                .filter(tx -> tx.getCreatedAt() != null)
                .filter(tx -> !tx.getCreatedAt().isBefore(weekStart))
                .filter(tx -> tx.getPoints() != null && tx.getPoints().compareTo(BigDecimal.ZERO) > 0)
                .map(PointTransactionEntity::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime findLastPointEarnedAt(String userId) {
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1))
                .stream()
                .map(PointTransactionEntity::getCreatedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
