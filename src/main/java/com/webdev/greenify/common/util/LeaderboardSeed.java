package com.webdev.greenify.common.util;

import com.webdev.greenify.leaderboard.entity.LeaderboardPrizeConfigEntity;
import com.webdev.greenify.leaderboard.entity.LeaderboardSnapshotEntity;
import com.webdev.greenify.leaderboard.enumeration.LeaderboardScope;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;
import com.webdev.greenify.leaderboard.repository.LeaderboardPrizeConfigRepository;
import com.webdev.greenify.leaderboard.repository.LeaderboardSnapshotRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import com.webdev.greenify.voucher.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardSeed {

    private static final long SEED_THRESHOLD = 5;

    private final LeaderboardPrizeConfigRepository prizeConfigRepository;
    private final LeaderboardSnapshotRepository snapshotRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final UserRepository userRepository;

    @Transactional
    public void seed() {
        if (snapshotRepository.count() > SEED_THRESHOLD) {
            log.info("Skip LeaderboardSeed because snapshot count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            VoucherTemplateEntity firstVoucherTemplate = voucherTemplateRepository.findAll().stream().findFirst().orElse(null);
            if (firstVoucherTemplate == null) {
                log.warn("Skip LeaderboardSeed because no voucher template found");
                return;
            }

            LocalDate lastMonday = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .minusWeeks(1);

            LeaderboardPrizeConfigEntity prizeConfig = prizeConfigRepository.findByWeekStartDate(lastMonday)
                    .orElseGet(() -> LeaderboardPrizeConfigEntity.builder()
                            .weekStartDate(lastMonday)
                            .build());

            prizeConfig.setWeekStartDate(lastMonday);
            prizeConfig.setLockAt(lastMonday.plusDays(6).atTime(23, 59));
            prizeConfig.setNationalVoucherTemplate(firstVoucherTemplate);
            prizeConfig.setProvincialVoucherTemplate(firstVoucherTemplate);
            prizeConfig.setNationalReservedCount(5);
            prizeConfig.setProvincialReservedCount(3);
            prizeConfig.setStatus(PrizeConfigStatus.DISTRIBUTED);
            prizeConfig.setDistributedAt(lastMonday.plusDays(6).atStartOfDay());
            prizeConfig = prizeConfigRepository.save(prizeConfig);

            List<LeaderboardSnapshotEntity> snapshots = new ArrayList<>();

            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.NATIONAL, null, 1, "ctv2", new BigDecimal("520.50"));
            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.NATIONAL, null, 2, "user1", new BigDecimal("420.00"));
            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.NATIONAL, null, 3, "user2", new BigDecimal("380.50"));
            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.NATIONAL, null, 4, "ctv1", new BigDecimal("210.00"));
            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.NATIONAL, null, 5, "user3", new BigDecimal("185.00"));

            String hcmProvince = "Thành phố Hồ Chí Minh";
            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.PROVINCIAL, hcmProvince, 1, "ctv2", new BigDecimal("520.50"));
            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.PROVINCIAL, hcmProvince, 2, "user1", new BigDecimal("420.00"));
            addSnapshotIfUserExists(snapshots, prizeConfig, lastMonday, LeaderboardScope.PROVINCIAL, hcmProvince, 3, "user6", new BigDecimal("95.00"));

            if (snapshots.isEmpty()) {
                log.warn("Skip LeaderboardSeed snapshots because no target users found");
                return;
            }

            snapshotRepository.saveAll(snapshots);
            log.info("LeaderboardSeed completed with {} snapshots", snapshots.size());
        } catch (Exception e) {
            log.warn("LeaderboardSeed failed: {}", e.getMessage(), e);
        }
    }

    private void addSnapshotIfUserExists(
            List<LeaderboardSnapshotEntity> snapshots,
            LeaderboardPrizeConfigEntity prizeConfig,
            LocalDate weekStartDate,
            LeaderboardScope scope,
            String province,
            int rank,
            String username,
            BigDecimal weeklyPoints) {

        UserEntity user = findUserByUsername(username);
        if (user == null) {
            log.warn("Skip leaderboard snapshot rank {} because user {} not found", rank, username);
            return;
        }

        LeaderboardSnapshotEntity snapshot = LeaderboardSnapshotEntity.builder()
                .prizeConfig(prizeConfig)
                .weekStartDate(weekStartDate)
                .scope(scope)
                .province(province)
                .rank(rank)
                .user(user)
                .weeklyPoints(weeklyPoints)
                .rewarded(true)
                .build();

        snapshots.add(snapshot);
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .or(() -> userRepository.findByIdentifier(username + "@greenify.vn"))
                .orElse(null);
    }
}
