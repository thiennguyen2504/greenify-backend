package com.webdev.greenify.garden.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.garden.dto.request.CreateSeedRequest;
import com.webdev.greenify.garden.dto.request.SelectSeedRequest;
import com.webdev.greenify.garden.dto.request.UpdateSeedRequest;
import com.webdev.greenify.garden.dto.response.GardenArchiveResponse;
import com.webdev.greenify.garden.dto.response.PlantDailyLogResponse;
import com.webdev.greenify.garden.dto.response.PlantProgressResponse;
import com.webdev.greenify.garden.dto.response.SeedResponse;
import com.webdev.greenify.garden.entity.GardenArchiveEntity;
import com.webdev.greenify.garden.entity.PlantDailyLogEntity;
import com.webdev.greenify.garden.entity.PlantProgressEntity;
import com.webdev.greenify.garden.entity.SeedEntity;
import com.webdev.greenify.garden.enumeration.GardenRewardStatus;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import com.webdev.greenify.garden.enumeration.PlantStage;
import com.webdev.greenify.garden.enumeration.PlantStatus;
import com.webdev.greenify.garden.mapper.GardenArchiveMapper;
import com.webdev.greenify.garden.mapper.PlantProgressMapper;
import com.webdev.greenify.garden.mapper.SeedMapper;
import com.webdev.greenify.garden.repository.GardenArchiveRepository;
import com.webdev.greenify.garden.repository.PlantDailyLogRepository;
import com.webdev.greenify.garden.repository.PlantProgressRepository;
import com.webdev.greenify.garden.repository.SeedRepository;
import com.webdev.greenify.garden.service.GardenService;
import com.webdev.greenify.garden.specification.PlantDailyLogSpecification;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.entity.UserVoucherEntity;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import com.webdev.greenify.voucher.enumeration.VoucherSource;
import com.webdev.greenify.voucher.mapper.VoucherMapper;
import com.webdev.greenify.voucher.repository.VoucherTemplateRepository;
import com.webdev.greenify.voucher.service.VoucherService;
import com.webdev.greenify.voucher.dto.response.VoucherTemplateResponse;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GardenServiceImpl implements GardenService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final SeedRepository seedRepository;
    private final PlantProgressRepository plantProgressRepository;
    private final PlantDailyLogRepository plantDailyLogRepository;
    private final GardenArchiveRepository gardenArchiveRepository;
    private final UserRepository userRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final VoucherService voucherService;
    private final VoucherMapper voucherMapper;
    private final SeedMapper seedMapper;
    private final PlantProgressMapper plantProgressMapper;
    private final GardenArchiveMapper gardenArchiveMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SeedResponse> getAvailableSeeds(int page, int size) {
        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(
                effectivePage,
                effectiveSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SeedEntity> seedsPage = seedRepository.findAllByIsActiveTrue(pageable);

        List<SeedResponse> content = seedsPage.getContent().stream()
                .map(seedMapper::toSeedResponse)
                .toList();

        return PagedResponse.of(
                content,
                seedsPage.getNumber(),
                seedsPage.getSize(),
                seedsPage.getTotalElements(),
                seedsPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherTemplateResponse getRewardVoucherTemplateBySeedId(String seedId) {
        SeedEntity seed = seedRepository.findByIdAndIsActiveTrue(seedId)
                .orElseThrow(() -> new ResourceNotFoundException("Seed not found or inactive"));

        VoucherTemplateEntity rewardVoucherTemplate = seed.getRewardVoucherTemplate();
        if (rewardVoucherTemplate == null) {
            throw new ResourceNotFoundException("Reward voucher template not configured for this seed");
        }

        return voucherMapper.toVoucherTemplateResponse(rewardVoucherTemplate);
    }

    @Override
    @Transactional
    public PlantProgressResponse selectSeed(SelectSeedRequest request) {
        String userId = getCurrentUserId();

        SeedEntity seed = seedRepository.findByIdAndIsActiveTrue(request.getSeedId())
                .orElseThrow(() -> new ResourceNotFoundException("Seed not found or inactive"));

        if (plantProgressRepository.findByUserIdAndStatus(userId, PlantStatus.GROWING).isPresent()) {
            throw new AppException("Bạn đang có cây đang phát triển", HttpStatus.CONFLICT);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PlantProgressEntity plantProgress = PlantProgressEntity.builder()
                .user(user)
                .seed(seed)
                .progressDays(0)
                .currentStage(PlantStage.SEED)
                .status(PlantStatus.GROWING)
                .startedAt(LocalDateTime.now())
                .build();

        plantProgress = plantProgressRepository.save(plantProgress);

        log.info("User {} selected seed {} and started a new plant progress {}",
                userId,
                seed.getId(),
                plantProgress.getId());

        return plantProgressMapper.toPlantProgressResponse(plantProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public PlantProgressResponse getCurrentPlantProgress() {
        String userId = getCurrentUserId();

        PlantProgressEntity progress = plantProgressRepository.findByUserIdAndStatus(userId, PlantStatus.GROWING)
                .orElseThrow(() -> new ResourceNotFoundException("No active plant progress found"));

        return plantProgressMapper.toPlantProgressResponse(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlantDailyLogResponse> getCurrentUserDailyLogs(LocalDate fromDate, LocalDate toDate) {
        String userId = getCurrentUserId();
        Specification<PlantDailyLogEntity> specification = PlantDailyLogSpecification.buildSpecification(
                userId,
                fromDate,
                toDate);

        List<PlantDailyLogEntity> dailyLogs = plantDailyLogRepository.findAll(
                specification,
                Sort.by(Sort.Direction.ASC, "logDate"));

        List<PlantDailyLogResponse> responses = new ArrayList<>(dailyLogs.size());
        PlantStage previousStage = null;

        for (PlantDailyLogEntity dailyLog : dailyLogs) {
            PlantStage currentStage = dailyLog.getStage();
            boolean isChangeState = previousStage == null || !Objects.equals(currentStage, previousStage);

            responses.add(PlantDailyLogResponse.builder()
                    .logDate(dailyLog.getLogDate())
                    .stage(currentStage)
                    .isActiveDay(dailyLog.getIsActiveDay())
                    .imageUrl(dailyLog.getImageUrl())
                    .greenPostUrl(dailyLog.getGreenPostUrl())
                    .isChangeState(isChangeState)
                    .build());

            previousStage = currentStage;
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GardenArchiveResponse> getGardenArchives(int page, int size) {
        String userId = getCurrentUserId();
        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(
                effectivePage,
                effectiveSize,
                Sort.by(Sort.Direction.DESC, "archivedAt"));

        Page<GardenArchiveEntity> archivesPage = gardenArchiveRepository.findByUserIdOrderByArchivedAtDesc(userId, pageable);

        List<GardenArchiveResponse> content = archivesPage.getContent().stream()
                .map(gardenArchiveMapper::toGardenArchiveResponse)
                .toList();

        return PagedResponse.of(
                content,
                archivesPage.getNumber(),
                archivesPage.getSize(),
                archivesPage.getTotalElements(),
                archivesPage.getTotalPages());
    }

    /**
     * Update plant progress for a verified action day.
     * Idempotency key is userId + actionDate via plant_daily_logs.
     */
    @Override
    @Transactional
    public void updatePlantProgress(String userId, LocalDate actionDate, String greenPostUrl) {
        if (userId == null || actionDate == null) {
            return;
        }

        if (plantDailyLogRepository.existsByUserIdAndLogDate(userId, actionDate)) {
            log.info("Skip plant progress update for user {} on {} because daily log already exists", userId, actionDate);
            return;
        }

        PlantProgressEntity progress = plantProgressRepository.findByUserIdAndStatus(userId, PlantStatus.GROWING)
                .orElse(null);

        if (progress == null) {
            return;
        }

        int missedDays = calculateMissedDays(progress, actionDate);
        int appliedPenaltyDays = resolvePenaltyDays(progress.getSeed().getCycleType(), missedDays);

        int currentProgressDays = valueOrZero(progress.getProgressDays());
        int progressAfterPenalty = Math.max(0, currentProgressDays - appliedPenaltyDays);
        int newProgressDays = progressAfterPenalty + 1;

        progress.setProgressDays(newProgressDays);

        PlantStage newStage = resolveStage(progress.getSeed(), newProgressDays);
        progress.setCurrentStage(newStage);

        progress = plantProgressRepository.save(progress);

        PlantDailyLogEntity dailyLog = PlantDailyLogEntity.builder()
                .user(progress.getUser())
                .plantProgress(progress)
                .logDate(actionDate)
                .stage(newStage)
                .isActiveDay(true)
                .imageUrl(seedMapper.resolveStageImageUrl(progress.getSeed(), newStage))
                .greenPostUrl(greenPostUrl)
                .build();

        plantDailyLogRepository.save(dailyLog);

        log.info("Updated plant progress {} for user {} on {}: previousProgressDays={}, missedDays={}, appliedPenaltyDays={}, progressAfterPenalty={}, newProgressDays={}, stage={}",
                progress.getId(),
                userId,
                actionDate,
            currentProgressDays,
            missedDays,
            appliedPenaltyDays,
            progressAfterPenalty,
                newProgressDays,
                newStage);

        if (newProgressDays >= progress.getSeed().getDaysToMature()) {
            maturePlant(progress);
        }
    }

    @Override
    @Transactional
    public SeedResponse createSeed(CreateSeedRequest request) {
        validateSeedThresholds(request);

        VoucherTemplateEntity rewardVoucherTemplate = null;
        if (request.getRewardVoucherTemplateId() != null && !request.getRewardVoucherTemplateId().isBlank()) {
            rewardVoucherTemplate = voucherTemplateRepository.findById(request.getRewardVoucherTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reward voucher template not found"));
        }

        SeedEntity seed = SeedEntity.builder()
                .name(request.getName())
                .stage1ImageUrl(request.getStage1ImageUrl())
                .stage2ImageUrl(request.getStage2ImageUrl())
                .stage3ImageUrl(request.getStage3ImageUrl())
                .stage4ImageUrl(request.getStage4ImageUrl())
                .daysToMature(request.getDaysToMature())
                .stage2FromDay(request.getStage2FromDay())
                .stage3FromDay(request.getStage3FromDay())
                .stage4FromDay(request.getStage4FromDay())
                .cycleType(request.getCycleType())
                .rewardVoucherTemplate(rewardVoucherTemplate)
                .isActive(true)
                .build();

        seed = seedRepository.save(seed);

        log.info("Admin created new seed {} with cycleType={} and rewardVoucherTemplateId={}",
                seed.getId(),
                seed.getCycleType(),
                rewardVoucherTemplate != null ? rewardVoucherTemplate.getId() : null);

        return seedMapper.toSeedResponse(seed);
    }

    @Override
    @Transactional
    public SeedResponse updateSeed(String seedId, UpdateSeedRequest request) {
        String adminId = getCurrentUserId();

        SeedEntity seed = seedRepository.findById(seedId)
                .orElseThrow(() -> new ResourceNotFoundException("Seed not found"));

        validateSeedThresholdsForUpdate(request, seed);

        Set<String> changedFields = collectUpdatedSeedFields(request);
        seedMapper.updateSeedFromDto(request, seed);

        if (request.getRewardVoucherTemplateId() != null) {
            String rewardVoucherTemplateId = request.getRewardVoucherTemplateId().trim();
            if (rewardVoucherTemplateId.isEmpty()) {
                seed.setRewardVoucherTemplate(null);
            } else {
                VoucherTemplateEntity rewardVoucherTemplate = voucherTemplateRepository.findById(rewardVoucherTemplateId)
                        .orElseThrow(() -> new ResourceNotFoundException("Reward voucher template not found"));
                seed.setRewardVoucherTemplate(rewardVoucherTemplate);
            }
        }

        seed = seedRepository.save(seed);

        log.info("Admin {} updated seed {} fields={} rewardVoucherTemplateId={} isActive={}",
                adminId,
                seed.getId(),
                changedFields,
                seed.getRewardVoucherTemplate() != null ? seed.getRewardVoucherTemplate().getId() : null,
                seed.getIsActive());

        return seedMapper.toSeedResponse(seed);
    }

    /**
     * Mature a plant, archive the result, then issue voucher reward if configured.
     */
    private void maturePlant(PlantProgressEntity progress) {
        if (progress.getStatus() == PlantStatus.MATURED) {
            return;
        }

        progress.setStatus(PlantStatus.MATURED);
        progress.setMaturedAt(LocalDateTime.now());
        progress = plantProgressRepository.save(progress);

        GardenArchiveEntity archive = GardenArchiveEntity.builder()
                .user(progress.getUser())
                .seed(progress.getSeed())
                .plantProgress(progress)
                .daysTaken(progress.getProgressDays())
                .rewardStatus(GardenRewardStatus.MATURED)
                .displayImageUrl(progress.getSeed().getStage4ImageUrl())
                .archivedAt(LocalDateTime.now())
                .build();

        if (progress.getSeed().getRewardVoucherTemplate() != null) {
            UserVoucherEntity rewardedVoucher = voucherService.grantVoucherToUser(
                    progress.getUser().getId(),
                    progress.getSeed().getRewardVoucherTemplate().getId(),
                    VoucherSource.GARDEN_REWARD);

            archive.setUserVoucher(rewardedVoucher);
            archive.setRewardStatus(GardenRewardStatus.REWARDED);
        }

        gardenArchiveRepository.save(archive);

        log.info("Plant progress {} matured for user {} and archived with status {}",
                progress.getId(),
                progress.getUser().getId(),
                archive.getRewardStatus());
    }

    private PlantStage resolveStage(SeedEntity seed, int progressDays) {
        if (progressDays < seed.getStage2FromDay()) {
            return PlantStage.SEED;
        }
        if (progressDays < seed.getStage3FromDay()) {
            return PlantStage.SPROUT;
        }
        if (progressDays < seed.getStage4FromDay()) {
            return PlantStage.GROWING;
        }
        return PlantStage.BLOOMING;
    }

    private int calculateMissedDays(PlantProgressEntity progress, LocalDate actionDate) {
        if (progress == null || actionDate == null) {
            return 0;
        }

        LocalDate lastActiveDate = plantDailyLogRepository
                .findTopByPlantProgressIdAndIsActiveDayTrueOrderByLogDateDesc(progress.getId())
                .map(PlantDailyLogEntity::getLogDate)
                .orElseGet(() -> progress.getStartedAt() != null
                        ? progress.getStartedAt().toLocalDate()
                        : actionDate);

        long gapDays = ChronoUnit.DAYS.between(lastActiveDate, actionDate) - 1;
        if (gapDays <= 0) {
            return 0;
        }

        return (int) Math.min(gapDays, Integer.MAX_VALUE);
    }

    private int resolvePenaltyDays(PlantCycleType cycleType, int missedDays) {
        if (missedDays <= 0) {
            return 0;
        }

        PlantCycleType effectiveCycleType = cycleType != null ? cycleType : PlantCycleType.EASY;

        return switch (effectiveCycleType) {
            case EASY -> missedDays;
            case MEDIUM -> Math.max(0, missedDays - 1);
            case HARD -> Math.max(0, missedDays - 2);
        };
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private void validateSeedThresholds(CreateSeedRequest request) {
        validateSeedThresholds(
                request.getDaysToMature(),
                request.getStage2FromDay(),
                request.getStage3FromDay(),
                request.getStage4FromDay());
    }

    private void validateSeedThresholdsForUpdate(UpdateSeedRequest request, SeedEntity seed) {
        if (request.getDaysToMature() == null
                && request.getStage2FromDay() == null
                && request.getStage3FromDay() == null
                && request.getStage4FromDay() == null) {
            return;
        }

        int daysToMature = request.getDaysToMature() != null ? request.getDaysToMature() : seed.getDaysToMature();
        int stage2 = request.getStage2FromDay() != null ? request.getStage2FromDay() : seed.getStage2FromDay();
        int stage3 = request.getStage3FromDay() != null ? request.getStage3FromDay() : seed.getStage3FromDay();
        int stage4 = request.getStage4FromDay() != null ? request.getStage4FromDay() : seed.getStage4FromDay();

        validateSeedThresholds(daysToMature, stage2, stage3, stage4);
    }

    private void validateSeedThresholds(int daysToMature, int stage2, int stage3, int stage4) {
        if (!(stage2 < stage3 && stage3 < stage4 && stage4 < daysToMature)) {
            throw new AppException("Stage thresholds must satisfy stage2 < stage3 < stage4 < daysToMature",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Set<String> collectUpdatedSeedFields(UpdateSeedRequest request) {
        Set<String> changedFields = new LinkedHashSet<>();

        if (request.getName() != null) {
            changedFields.add("name");
        }
        if (request.getStage1Image() != null) {
            changedFields.add("stage1Image");
        }
        if (request.getStage2Image() != null) {
            changedFields.add("stage2Image");
        }
        if (request.getStage3Image() != null) {
            changedFields.add("stage3Image");
        }
        if (request.getStage4Image() != null) {
            changedFields.add("stage4Image");
        }
        if (request.getDaysToMature() != null) {
            changedFields.add("daysToMature");
        }
        if (request.getStage2FromDay() != null) {
            changedFields.add("stage2FromDay");
        }
        if (request.getStage3FromDay() != null) {
            changedFields.add("stage3FromDay");
        }
        if (request.getStage4FromDay() != null) {
            changedFields.add("stage4FromDay");
        }
        if (request.getCycleType() != null) {
            changedFields.add("cycleType");
        }
        if (request.getRewardVoucherTemplateId() != null) {
            changedFields.add("rewardVoucherTemplateId");
        }
        if (request.getIsActive() != null) {
            changedFields.add("isActive");
        }

        return changedFields;
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
