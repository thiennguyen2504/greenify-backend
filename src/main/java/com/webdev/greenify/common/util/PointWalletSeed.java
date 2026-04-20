package com.webdev.greenify.common.util;

import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointWalletSeed {

    private static final long SEED_THRESHOLD = 5;
    private static final String WALLET_STATUS_ACTIVE = "ACTIVE";

    private final PointWalletRepository pointWalletRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final GreenActionPostRepository postRepository;

    @Transactional
    public void seed() {
        if (pointWalletRepository.count() > SEED_THRESHOLD) {
            log.info("Skip PointWalletSeed because wallet count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            List<UserEntity> users = userRepository.findAll();
            if (users.isEmpty()) {
                log.warn("Skip PointWalletSeed because no users found");
                return;
            }

            List<String> postIds = postRepository.findAll().stream()
                    .map(GreenActionPostEntity::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (postIds.isEmpty()) {
                log.warn("Skip PointWalletSeed because no posts found to reference");
                return;
            }

            int postPointer = 0;
            for (UserEntity user : users) {
                try {
                    List<PointTransactionEntity> seededTransactions = new ArrayList<>();
                    seededTransactions.add(savePointTransaction(
                            user,
                            new BigDecimal("5.00"),
                            "Khởi động tài khoản Greenify",
                            LocalDateTime.now().minusDays(30),
                            postIds.get(postPointer++ % postIds.size())));

                    seededTransactions.add(savePointTransaction(
                            user,
                            new BigDecimal("5.00"),
                            "Hoàn thành hành động xanh: Phân loại rác tại nhà",
                            LocalDateTime.now().minusDays(25),
                            postIds.get(postPointer++ % postIds.size())));

                    seededTransactions.add(savePointTransaction(
                            user,
                            new BigDecimal("3.00"),
                            "Hoàn thành hành động xanh: Đi bộ thay xe máy",
                            LocalDateTime.now().minusDays(20),
                            postIds.get(postPointer++ % postIds.size())));

                    if (isCtv(user)) {
                        seededTransactions.add(savePointTransaction(
                                user,
                                new BigDecimal("1.00"),
                                "Duyệt bài hợp lệ với tư cách CTV",
                                LocalDateTime.now().minusDays(15),
                                postIds.get(postPointer++ % postIds.size())));
                    }

                    seededTransactions.add(savePointTransaction(
                            user,
                            new BigDecimal("100.00"),
                            "Tham gia sự kiện: Dọn dẹp bãi biển Vũng Tàu",
                            LocalDateTime.now().minusDays(10),
                            postIds.get(postPointer++ % postIds.size())));

                    seededTransactions.add(savePointTransaction(
                            user,
                            new BigDecimal("3.00"),
                            "Hoàn thành hành động xanh: Từ chối túi ni-lông",
                            LocalDateTime.now().minusDays(7),
                            postIds.get(postPointer++ % postIds.size())));

                    seededTransactions.add(savePointTransaction(
                            user,
                            new BigDecimal("7.00"),
                            "Hoàn thành hành động xanh: Trồng cây gây rừng",
                            LocalDateTime.now().minusDays(3),
                            postIds.get(postPointer++ % postIds.size())));

                    synchronizeWallet(user);

                    log.info("Seeded point wallet and {} transactions for user {}",
                            seededTransactions.size(),
                            user.getUsername());
                } catch (Exception ex) {
                    log.warn("Skip wallet seed for user {} due to error: {}", user.getUsername(), ex.getMessage());
                }
            }

            log.info("PointWalletSeed completed");
        } catch (Exception e) {
            log.warn("PointWalletSeed failed: {}", e.getMessage(), e);
        }
    }

    private PointTransactionEntity savePointTransaction(
            UserEntity user,
            BigDecimal points,
            String description,
            LocalDateTime createdAt,
            String sourcePostId) {

        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .user(user)
                .points(points)
                .actionDescription(description)
                .sourcePostId(sourcePostId)
                .expiresAt(createdAt.plusMonths(2))
                .build();

        transaction = pointTransactionRepository.save(transaction);
        transaction.setCreatedAt(createdAt);

        return pointTransactionRepository.save(transaction);
    }

    private void synchronizeWallet(UserEntity user) {
        BigDecimal availablePoints = pointTransactionRepository.sumAvailablePointsByUserId(user.getId());
        BigDecimal totalPoints = pointTransactionRepository.sumAccumulatedPointsByUserId(user.getId());

        PointWalletEntity wallet = pointWalletRepository.findByUserId(user.getId())
                .orElseGet(() -> PointWalletEntity.builder().user(user).build());

        wallet.setUser(user);
        wallet.setAvailablePoints(defaultZero(availablePoints));
        wallet.setTotalPoints(defaultZero(totalPoints));
        wallet.setWeeklyPoints(calculateWeeklyPoints(user.getId()));
        wallet.setLastPointEarnedAt(findLastPointEarnedAt(user.getId()));
        wallet.setStatus(WALLET_STATUS_ACTIVE);

        pointWalletRepository.save(wallet);
    }

    private BigDecimal calculateWeeklyPoints(String userId) {
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);

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

    private boolean isCtv(UserEntity user) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> "CTV".equalsIgnoreCase(role.getName()));
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
