package com.webdev.greenify.common.util;

import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.leaderboard.entity.LeaderboardPrizeConfigEntity;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;
import com.webdev.greenify.leaderboard.repository.LeaderboardPrizeConfigRepository;
import com.webdev.greenify.point.entity.PointWalletEntity;
import com.webdev.greenify.point.repository.PointWalletRepository;
import com.webdev.greenify.station.service.ProvinceNormalizationService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.UserProfileEntity;
import com.webdev.greenify.user.repository.UserProfileRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import com.webdev.greenify.voucher.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardCurrentWeekSeed {

    private static final LocalDate CURRENT_WEEK_START = LocalDate.of(2026, 4, 20);
    private static final String NATIONAL_KEY = "leaderboard:weekly:national:" + CURRENT_WEEK_START;
    private static final String PROVINCIAL_PREFIX = "leaderboard:weekly:province:";
    private static final Duration LEADERBOARD_TTL = Duration.ofDays(14);
    private static final String WALLET_STATUS_ACTIVE = "ACTIVE";

    private final LeaderboardPrizeConfigRepository prizeConfigRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ProvinceNormalizationService provinceNormalizationService;

    @Transactional
    public void seed() {
        try {
            seedPrizeConfigIfMissing();
            List<SeededWalletScore> seededScores = seedWalletWeeklyPoints();
            seedRedisScores(seededScores);

            log.info("LeaderboardCurrentWeekSeed completed (week={}, users={})", CURRENT_WEEK_START, seededScores.size());
        } catch (Exception e) {
            log.warn("LeaderboardCurrentWeekSeed failed: {}", e.getMessage(), e);
        }
    }

    private void seedPrizeConfigIfMissing() {
        if (prizeConfigRepository.findByWeekStartDate(CURRENT_WEEK_START).isPresent()) {
            log.info("Skip prize config insert for week {} - already exists", CURRENT_WEEK_START);
            return;
        }

        List<VoucherTemplateEntity> templates = voucherTemplateRepository.findAll();
        if (templates.isEmpty()) {
            log.warn("Skip LeaderboardCurrentWeekSeed prize config - no voucher templates found");
            return;
        }

        VoucherTemplateEntity nationalVoucherTemplate = selectTemplate(templates, "highlands");
        VoucherTemplateEntity provincialVoucherTemplate = selectTemplate(templates, "coffee house");

        LeaderboardPrizeConfigEntity config = LeaderboardPrizeConfigEntity.builder()
                .weekStartDate(CURRENT_WEEK_START)
                .lockAt(LocalDateTime.of(2026, 4, 26, 23, 59))
                .status(PrizeConfigStatus.CONFIGURED)
                .nationalReservedCount(10)
                .provincialReservedCount(340)
                .nationalVoucherTemplate(nationalVoucherTemplate)
                .provincialVoucherTemplate(provincialVoucherTemplate)
                .build();

        prizeConfigRepository.save(config);
        log.info("Seeded leaderboard prize config for week {}", CURRENT_WEEK_START);
    }

    private VoucherTemplateEntity selectTemplate(List<VoucherTemplateEntity> templates, String keyword) {
        return templates.stream()
                .filter(template -> template.getName() != null)
                .filter(template -> template.getName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(templates.get(0));
    }

    private List<SeededWalletScore> seedWalletWeeklyPoints() {
        LocalDateTime now = LocalDateTime.now();

        List<WalletSeed> targets = List.of(
                new WalletSeed("ctv2@greenify.vn", new BigDecimal("245.50"), now),
                new WalletSeed("user1@greenify.vn", new BigDecimal("198.00"), now.minusHours(2)),
                new WalletSeed("ctv1@greenify.vn", new BigDecimal("175.00"), now.minusHours(5)),
                new WalletSeed("user2@greenify.vn", new BigDecimal("132.50"), now.minusHours(8)),
                new WalletSeed("user3@greenify.vn", new BigDecimal("98.00"), now.minusDays(1)),
                new WalletSeed("ctv3@greenify.vn", new BigDecimal("87.00"), now.minusDays(1)),
                new WalletSeed("user4@greenify.vn", new BigDecimal("65.00"), now.minusDays(2)),
                new WalletSeed("user5@greenify.vn", new BigDecimal("43.00"), now.minusDays(2)),
                new WalletSeed("user6@greenify.vn", new BigDecimal("32.00"), now.minusDays(3)),
                new WalletSeed("user7@greenify.vn", new BigDecimal("21.50"), now.minusDays(3))
        );

        List<SeededWalletScore> seeded = new ArrayList<>();
        for (WalletSeed target : targets) {
            UserEntity user = userRepository.findByIdentifier(target.email()).orElse(null);
            if (user == null) {
                log.warn("Skip weekly wallet seed because user {} not found", target.email());
                continue;
            }

            PointWalletEntity wallet = pointWalletRepository.findByUserId(user.getId())
                    .orElseGet(() -> buildNewWallet(user));

            ensureWalletTotals(wallet, user);
            wallet.setWeeklyPoints(target.weeklyPoints());
            wallet.setLastPointEarnedAt(target.lastPointEarnedAt());
            if (wallet.getStatus() == null || wallet.getStatus().isBlank()) {
                wallet.setStatus(WALLET_STATUS_ACTIVE);
            }

            pointWalletRepository.save(wallet);
            seeded.add(new SeededWalletScore(user.getId(), target.weeklyPoints(), target.lastPointEarnedAt()));
        }

        return seeded;
    }

    private PointWalletEntity buildNewWallet(UserEntity user) {
        return PointWalletEntity.builder()
                .user(user)
                .availablePoints(defaultZero(pointTransactionRepository.sumAvailablePointsByUserId(user.getId())))
                .totalPoints(defaultZero(pointTransactionRepository.sumAccumulatedPointsByUserId(user.getId())))
                .weeklyPoints(BigDecimal.ZERO)
                .status(WALLET_STATUS_ACTIVE)
                .build();
    }

    private void ensureWalletTotals(PointWalletEntity wallet, UserEntity user) {
        if (wallet.getAvailablePoints() == null) {
            wallet.setAvailablePoints(defaultZero(pointTransactionRepository.sumAvailablePointsByUserId(user.getId())));
        }
        if (wallet.getTotalPoints() == null) {
            wallet.setTotalPoints(defaultZero(pointTransactionRepository.sumAccumulatedPointsByUserId(user.getId())));
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void seedRedisScores(List<SeededWalletScore> seededScores) {
        if (seededScores.isEmpty()) {
            return;
        }

        try {
            boolean skipNational = hasAtLeastFiveMembers(NATIONAL_KEY);
            if (skipNational) {
                log.info("Skip national redis seed for key {} because it already has at least 5 members", NATIONAL_KEY);
            }

            Map<String, Boolean> skipProvinceByKey = new HashMap<>();

            for (SeededWalletScore seededScore : seededScores) {
                double compositeScore = buildCompositeScore(seededScore.weeklyPoints(), seededScore.lastPointEarnedAt());

                if (!skipNational) {
                    stringRedisTemplate.opsForZSet().add(NATIONAL_KEY, seededScore.userId(), compositeScore);
                    stringRedisTemplate.expire(NATIONAL_KEY, LEADERBOARD_TTL);
                }

                userProfileRepository.findByUserId(seededScore.userId())
                        .map(UserProfileEntity::getProvince)
                        .map(provinceNormalizationService::normalizeProvinceName)
                        .filter(this::hasText)
                        .ifPresent(province -> {
                            String provinceKey = buildProvinceKey(province);
                            boolean skipProvince = skipProvinceByKey.computeIfAbsent(provinceKey, this::hasAtLeastFiveMembers);
                            if (skipProvince) {
                                return;
                            }

                            stringRedisTemplate.opsForZSet().add(provinceKey, seededScore.userId(), compositeScore);
                            stringRedisTemplate.expire(provinceKey, LEADERBOARD_TTL);
                        });
            }
        } catch (Exception ex) {
            log.warn("LeaderboardCurrentWeekSeed redis write failed: {}", ex.getMessage(), ex);
        }
    }

    private boolean hasAtLeastFiveMembers(String key) {
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        return count != null && count >= 5;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildProvinceKey(String province) {
        return PROVINCIAL_PREFIX + province + ":" + CURRENT_WEEK_START;
    }

    private double buildCompositeScore(BigDecimal weeklyPoints, LocalDateTime lastPointEarnedAt) {
        long epochMilli = java.sql.Timestamp.valueOf(lastPointEarnedAt).getTime();
        double tieBreakerComponent = (Long.MAX_VALUE - epochMilli) / 1_000_000_000D;
        return weeklyPoints.doubleValue() * 1_000_000D + tieBreakerComponent;
    }

    private record WalletSeed(String email, BigDecimal weeklyPoints, LocalDateTime lastPointEarnedAt) {
    }

    private record SeededWalletScore(String userId, BigDecimal weeklyPoints, LocalDateTime lastPointEarnedAt) {
    }
}