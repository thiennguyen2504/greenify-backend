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
        try {
            Map<String, SeedEntity> seedsByName = seedRepository.findAll().stream()
                    .collect(Collectors.toMap(SeedEntity::getName, Function.identity(), (left, right) -> left));

            boolean shouldSeedProgress = plantProgressRepository.count() < SEED_THRESHOLD;
            if (shouldSeedProgress) {
                seedScenarioUser1(seedsByName);
                seedScenarioUser2(seedsByName);
                seedScenarioUser3(seedsByName);
                seedScenarioUser4(seedsByName);
                seedScenarioUser5(seedsByName);
            }

            seedArchiveScenario("user6", "Hoa lan", 70, 2, true);
            seedArchiveScenario("user7", "Cây táo", 100, 3, true);
            seedArchiveScenario("user", "Hướng dương", 30, 2, false);
            seedArchiveScenario("user", "Hoa hồng", 40, 3, false);
            seedArchiveScenario("user", "Tre", 80, 4, false);
            
            // Add 5 more archive gardens for user "user" using existing seeds
            seedArchiveScenario("user", "Hoa tulip", 30, 5, false);
            seedArchiveScenario("user", "Hoa mai", 60, 6, false);
            seedArchiveScenario("user", "Cẩm chướng", 45, 7, false);
            seedArchiveScenario("user", "Hoa lan", 70, 8, false);
            seedArchiveScenario("user", "Xương rồng nở hoa", 60, 9, false);

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
            if (plantProgressRepository.existsByUserId(user.getId())) {
                log.info("Skip garden scenario user1 because plant progress already exists for user {}", user.getUsername());
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
            if (plantProgressRepository.existsByUserId(user.getId())) {
                log.info("Skip garden scenario user2 because plant progress already exists for user {}", user.getUsername());
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
            if (plantProgressRepository.existsByUserId(user.getId())) {
                log.info("Skip garden scenario user3 because plant progress already exists for user {}", user.getUsername());
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
            if (plantProgressRepository.existsByUserId(user.getId())) {
                log.info("Skip garden scenario user4 because plant progress already exists for user {}", user.getUsername());
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
            if (plantProgressRepository.existsByUserId(user.getId())) {
                log.info("Skip garden scenario user5 because plant progress already exists for user {}", user.getUsername());
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

    private void seedArchiveScenario(
            String username,
            String seedName,
            int daysTaken,
            int archivedDaysAgo,
            boolean skipIfProgressExists) {
        try {
            UserEntity user = findUserByUsername(username);
            SeedEntity seed = seedRepository.findAll().stream()
                    .filter(item -> seedName.equals(item.getName()))
                    .findFirst()
                    .orElse(null);
            if (user == null || seed == null) {
                log.warn("Skip garden archive scenario for {} because user/seed is missing", username);
                return;
            }

            if (gardenArchiveRepository.existsByUserIdAndSeedId(user.getId(), seed.getId())) {
                log.info("Skip garden archive scenario for {} because archive already exists for seed {}",
                        username,
                        seedName);
                return;
            }

            if (skipIfProgressExists && plantProgressRepository.existsByUserId(user.getId())) {
                log.info("Skip garden archive scenario for {} because plant progress already exists", username);
                return;
            }

            LocalDateTime startedAt = LocalDateTime.now().minusDays(daysTaken + archivedDaysAgo);
            PlantProgressEntity progress = PlantProgressEntity.builder()
                    .user(user)
                    .seed(seed)
                    .startedAt(startedAt)
                    .progressDays(daysTaken)
                    .currentStage(PlantStage.BLOOMING)
                    .status(PlantStatus.MATURED)
                    .maturedAt(startedAt.plusDays(daysTaken))
                    .build();
            progress = plantProgressRepository.save(progress);

            String plantImageUrl = getPlantImageUrl(seed);
            int logDays = Math.min(daysTaken, 10);
            for (int day = 1; day <= logDays; day++) {
                PlantStage stage = resolveStageByDay(seed, day);
                saveDailyLog(user, progress, startedAt.toLocalDate().plusDays(day - 1), stage, true, plantImageUrl);
            }

            GardenArchiveEntity archive = GardenArchiveEntity.builder()
                    .user(user)
                    .seed(seed)
                    .plantProgress(progress)
                    .daysTaken(daysTaken)
                    .rewardStatus(GardenRewardStatus.MATURED)
                    .displayImageUrl(seed.getStage4ImageUrl())
                    .archivedAt(LocalDateTime.now().minusDays(archivedDaysAgo))
                    .build();
            gardenArchiveRepository.save(archive);

            log.info("Seeded garden archive scenario for {}", username);
        } catch (Exception ex) {
            log.warn("Failed garden archive scenario for {}: {}", username, ex.getMessage());
        }
    }

    private void saveDailyLog(
            UserEntity user,
            PlantProgressEntity progress,
            LocalDate logDate,
            PlantStage stage,
            boolean isActiveDay,
            String imageUrl) {

        if (plantDailyLogRepository.existsByUserIdAndLogDate(user.getId(), logDate)) {
            return;
        }

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
