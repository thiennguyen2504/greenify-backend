package com.webdev.greenify.common.util;

import com.webdev.greenify.garden.entity.GardenArchiveEntity;
import com.webdev.greenify.garden.entity.PlantDailyLogEntity;
import com.webdev.greenify.garden.entity.PlantProgressEntity;
import com.webdev.greenify.garden.entity.SeedEntity;
import com.webdev.greenify.garden.enumeration.GardenRewardStatus;
import com.webdev.greenify.garden.enumeration.PlantStage;
import com.webdev.greenify.garden.enumeration.PlantStatus;
import com.webdev.greenify.garden.repository.GardenArchiveRepository;
import com.webdev.greenify.garden.repository.PlantDailyLogRepository;
import com.webdev.greenify.garden.repository.PlantProgressRepository;
import com.webdev.greenify.garden.repository.SeedRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.entity.UserVoucherEntity;
import com.webdev.greenify.voucher.enumeration.VoucherSource;
import com.webdev.greenify.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GardenSeed {

    private static final long SEED_THRESHOLD = 5;

    private final PlantProgressRepository plantProgressRepository;
    private final PlantDailyLogRepository plantDailyLogRepository;
    private final GardenArchiveRepository gardenArchiveRepository;
    private final SeedRepository seedRepository;
    private final UserRepository userRepository;
    private final VoucherService voucherService;
    private final UnsplashImageService unsplashImageService;

    @Transactional
    public void seed() {
        if (plantProgressRepository.count() > SEED_THRESHOLD) {
            log.info("Skip GardenSeed because plant progress count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            Map<String, SeedEntity> seedsByName = seedRepository.findAll().stream()
                    .collect(Collectors.toMap(SeedEntity::getName, Function.identity(), (left, right) -> left));

            seedScenarioUser1(seedsByName);
            seedScenarioUser2(seedsByName);
            seedScenarioUser3(seedsByName);
            seedScenarioUser4(seedsByName);
            seedScenarioUser5(seedsByName);

            log.info("GardenSeed completed");
        } catch (Exception e) {
            log.warn("GardenSeed failed: {}", e.getMessage(), e);
        }
    }

    private void seedScenarioUser1(Map<String, SeedEntity> seedsByName) {
        try {
            UserEntity user = findUserByUsername("user1");
            SeedEntity seed = seedsByName.get("Hoa hồng");
            if (user == null || seed == null) {
                log.warn("Skip garden scenario user1 because user/seed is missing");
                return;
            }

            LocalDateTime startedAt = LocalDateTime.now().minusDays(15);
            PlantProgressEntity progress = PlantProgressEntity.builder()
                    .user(user)
                    .seed(seed)
                    .startedAt(startedAt)
                    .progressDays(15)
                    .currentStage(PlantStage.SPROUT)
                    .status(PlantStatus.GROWING)
                    .build();
            progress = plantProgressRepository.save(progress);

            String plantImageUrl = getPlantImageUrl(seed);

            for (int day = 1; day <= 15; day++) {
                PlantStage stage = day <= 3 ? PlantStage.SEED : PlantStage.SPROUT;
                saveDailyLog(user, progress, startedAt.toLocalDate().plusDays(day - 1), stage, true, plantImageUrl);
            }

            log.info("Seeded garden scenario for user1");
        } catch (Exception ex) {
            log.warn("Failed garden scenario user1: {}", ex.getMessage());
        }
    }

    private void seedScenarioUser2(Map<String, SeedEntity> seedsByName) {
        try {
            UserEntity user = findUserByUsername("user2");
            SeedEntity seed = seedsByName.get("Hướng dương");
            if (user == null || seed == null) {
                log.warn("Skip garden scenario user2 because user/seed is missing");
                return;
            }

            LocalDateTime startedAt = LocalDateTime.now().minusDays(30);
            PlantProgressEntity progress = PlantProgressEntity.builder()
                    .user(user)
                    .seed(seed)
                    .startedAt(startedAt)
                    .progressDays(30)
                    .currentStage(PlantStage.BLOOMING)
                    .status(PlantStatus.MATURED)
                    .maturedAt(startedAt.plusDays(30))
                    .build();
            progress = plantProgressRepository.save(progress);

            String plantImageUrl = getPlantImageUrl(seed);

            for (int day = 1; day <= 30; day++) {
                PlantStage stage = resolveStageByDay(seed, day);
                saveDailyLog(user, progress, startedAt.toLocalDate().plusDays(day - 1), stage, true, plantImageUrl);
            }

            UserVoucherEntity grantedVoucher = null;
            try {
                if (seed.getRewardVoucherTemplate() != null) {
                    grantedVoucher = voucherService.grantVoucherToUser(
                            user.getId(),
                            seed.getRewardVoucherTemplate().getId(),
                            VoucherSource.GARDEN_REWARD);
                } else {
                    log.warn("Seed {} has no reward voucher template", seed.getName());
                }
            } catch (Exception ex) {
                log.warn("Could not grant garden reward voucher for user2: {}", ex.getMessage());
            }

            GardenArchiveEntity archive = GardenArchiveEntity.builder()
                    .user(user)
                    .seed(seed)
                    .plantProgress(progress)
                    .daysTaken(30)
                    .rewardStatus(GardenRewardStatus.REWARDED)
                    .userVoucher(grantedVoucher)
                    .displayImageUrl(seed.getStage4ImageUrl())
                    .archivedAt(LocalDateTime.now().minusDays(1))
                    .build();
            gardenArchiveRepository.save(archive);

            log.info("Seeded garden scenario for user2");
        } catch (Exception ex) {
            log.warn("Failed garden scenario user2: {}", ex.getMessage());
        }
    }

    private void seedScenarioUser3(Map<String, SeedEntity> seedsByName) {
        try {
            UserEntity user = findUserByUsername("user3");
            SeedEntity seed = seedsByName.get("Cây thông");
            if (user == null || seed == null) {
                log.warn("Skip garden scenario user3 because user/seed is missing");
                return;
            }

            LocalDateTime startedAt = LocalDateTime.now().minusDays(50);
            PlantProgressEntity progress = PlantProgressEntity.builder()
                    .user(user)
                    .seed(seed)
                    .startedAt(startedAt)
                    .progressDays(45)
                    .currentStage(PlantStage.GROWING)
                    .status(PlantStatus.GROWING)
                    .build();
            progress = plantProgressRepository.save(progress);

            String plantImageUrl = getPlantImageUrl(seed);

            for (int day = 1; day <= 50; day++) {
                PlantStage stage = resolveStageByDay(seed, day);
                boolean isActiveDay = day <= 45;
                saveDailyLog(user, progress, startedAt.toLocalDate().plusDays(day - 1), stage, isActiveDay, plantImageUrl);
            }

            log.info("Seeded garden scenario for user3");
        } catch (Exception ex) {
            log.warn("Failed garden scenario user3: {}", ex.getMessage());
        }
    }

    private void seedScenarioUser4(Map<String, SeedEntity> seedsByName) {
        try {
            UserEntity user = findUserByUsername("user4");
            SeedEntity seed = seedsByName.get("Hoa tulip");
            if (user == null || seed == null) {
                log.warn("Skip garden scenario user4 because user/seed is missing");
                return;
            }

            LocalDateTime startedAt = LocalDateTime.now().minusDays(3);
            PlantProgressEntity progress = PlantProgressEntity.builder()
                    .user(user)
                    .seed(seed)
                    .startedAt(startedAt)
                    .progressDays(3)
                    .currentStage(PlantStage.SEED)
                    .status(PlantStatus.GROWING)
                    .build();
            progress = plantProgressRepository.save(progress);

            String plantImageUrl = getPlantImageUrl(seed);

            for (int day = 1; day <= 3; day++) {
                saveDailyLog(user, progress, startedAt.toLocalDate().plusDays(day - 1), PlantStage.SEED, true, plantImageUrl);
            }

            log.info("Seeded garden scenario for user4");
        } catch (Exception ex) {
            log.warn("Failed garden scenario user4: {}", ex.getMessage());
        }
    }

    private void seedScenarioUser5(Map<String, SeedEntity> seedsByName) {
        try {
            UserEntity user = findUserByUsername("user5");
            SeedEntity seed = seedsByName.get("Sen");
            if (user == null || seed == null) {
                log.warn("Skip garden scenario user5 because user/seed is missing");
                return;
            }

            LocalDateTime startedAt = LocalDateTime.now().minusDays(25);
            PlantProgressEntity progress = PlantProgressEntity.builder()
                    .user(user)
                    .seed(seed)
                    .startedAt(startedAt)
                    .progressDays(25)
                    .currentStage(PlantStage.GROWING)
                    .status(PlantStatus.GROWING)
                    .build();
            progress = plantProgressRepository.save(progress);

            String plantImageUrl = getPlantImageUrl(seed);

            for (int day = 1; day <= 25; day++) {
                PlantStage stage = day <= 3 ? PlantStage.SEED : (day <= 10 ? PlantStage.SPROUT : PlantStage.GROWING);
                saveDailyLog(user, progress, startedAt.toLocalDate().plusDays(day - 1), stage, true, plantImageUrl);
            }

            log.info("Seeded garden scenario for user5");
        } catch (Exception ex) {
            log.warn("Failed garden scenario user5: {}", ex.getMessage());
        }
    }

    private void saveDailyLog(
            UserEntity user,
            PlantProgressEntity progress,
            LocalDate logDate,
            PlantStage stage,
            boolean isActiveDay,
            String imageUrl) {

        PlantDailyLogEntity logEntity = PlantDailyLogEntity.builder()
                .user(user)
                .plantProgress(progress)
                .logDate(logDate)
                .stage(stage)
                .isActiveDay(isActiveDay)
                .imageUrl(imageUrl)
                .build();

        plantDailyLogRepository.save(logEntity);
    }

    private String getPlantImageUrl(SeedEntity seed) {
        String keyword = UnsplashKeywordMapper.getPlantKeyword(seed != null ? seed.getName() : null);
        return unsplashImageService.getImageUrl(keyword);
    }

    private PlantStage resolveStageByDay(SeedEntity seed, int day) {
        if (day >= seed.getStage4FromDay()) {
            return PlantStage.BLOOMING;
        }
        if (day >= seed.getStage3FromDay()) {
            return PlantStage.GROWING;
        }
        if (day >= seed.getStage2FromDay()) {
            return PlantStage.SPROUT;
        }
        return PlantStage.SEED;
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .or(() -> userRepository.findByIdentifier(username + "@greenify.vn"))
                .orElse(null);
    }
}
