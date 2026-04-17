package com.webdev.greenify.leaderboard.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.leaderboard.dto.request.CreatePrizeConfigRequest;
import com.webdev.greenify.leaderboard.dto.response.LeaderboardEntryResponse;
import com.webdev.greenify.leaderboard.dto.response.LeaderboardPrizeResponse;
import com.webdev.greenify.leaderboard.dto.response.LeaderboardResponse;
import com.webdev.greenify.leaderboard.dto.response.PrizeConfigResponse;
import com.webdev.greenify.leaderboard.entity.LeaderboardPrizeConfigEntity;
import com.webdev.greenify.leaderboard.entity.LeaderboardSnapshotEntity;
import com.webdev.greenify.leaderboard.enumeration.LeaderboardScope;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;
import com.webdev.greenify.leaderboard.mapper.LeaderboardMapper;
import com.webdev.greenify.leaderboard.repository.LeaderboardPrizeConfigRepository;
import com.webdev.greenify.leaderboard.repository.LeaderboardSnapshotRepository;
import com.webdev.greenify.leaderboard.service.LeaderboardService;
import com.webdev.greenify.point.entity.PointWalletEntity;
import com.webdev.greenify.point.repository.PointWalletRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.UserProfileEntity;
import com.webdev.greenify.user.repository.UserProfileRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.entity.UserVoucherEntity;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import com.webdev.greenify.voucher.enumeration.VoucherSource;
import com.webdev.greenify.voucher.mapper.VoucherMapper;
import com.webdev.greenify.voucher.repository.VoucherTemplateRepository;
import com.webdev.greenify.voucher.service.VoucherService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final int TOP_LIMIT = 10;
    private static final int DEFAULT_NATIONAL_RESERVED_COUNT = 10;
    private static final int DEFAULT_PROVINCIAL_RESERVED_COUNT = 340;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final double SCORE_MULTIPLIER = 1_000_000D;
    private static final double TIE_BREAK_DIVISOR = 1_000_000_000D;
    private static final Duration LEADERBOARD_TTL = Duration.ofDays(14);
    private static final String NATIONAL_KEY_PREFIX = "leaderboard:weekly:national:";
    private static final String PROVINCIAL_KEY_PREFIX = "leaderboard:weekly:province:";

    private final LeaderboardPrizeConfigRepository leaderboardPrizeConfigRepository;
    private final LeaderboardSnapshotRepository leaderboardSnapshotRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final PointWalletRepository pointWalletRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final VoucherService voucherService;
    private final VoucherMapper voucherMapper;
    private final LeaderboardMapper leaderboardMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public PrizeConfigResponse createPrizeConfig(CreatePrizeConfigRequest request) {
        LocalDate weekStartDate = parseWeekStartDate(request.getWeekStartDate());
        LocalDateTime lockAt = parseLockAt(request.getLockAt());
        validateConfigWindow(weekStartDate, lockAt);

        LeaderboardPrizeConfigEntity existingConfig = leaderboardPrizeConfigRepository
                .findByWeekStartDate(weekStartDate)
                .orElse(null);

        if (existingConfig != null && existingConfig.getStatus() == PrizeConfigStatus.DISTRIBUTED) {
            throw new AppException("Prize config for this week was already distributed", HttpStatus.BAD_REQUEST);
        }

        if (existingConfig != null && existingConfig.getStatus() == PrizeConfigStatus.CONFIGURED) {
            releaseReservedStock(existingConfig);
            log.info("Released reserved stock from old leaderboard config {} before replacement", existingConfig.getId());
        }

        LockedTemplates lockedTemplates = lockTemplates(
                request.getNationalVoucherTemplateId(),
                request.getProvincialVoucherTemplateId());

        reserveStock(lockedTemplates.nationalTemplate(), lockedTemplates.provincialTemplate());

        LeaderboardPrizeConfigEntity entity = existingConfig != null
                ? existingConfig
                : LeaderboardPrizeConfigEntity.builder().build();

        entity.setWeekStartDate(weekStartDate);
        entity.setLockAt(lockAt);
        entity.setStatus(PrizeConfigStatus.CONFIGURED);
        entity.setNationalVoucherTemplate(lockedTemplates.nationalTemplate());
        entity.setProvincialVoucherTemplate(lockedTemplates.provincialTemplate());
        entity.setNationalReservedCount(DEFAULT_NATIONAL_RESERVED_COUNT);
        entity.setProvincialReservedCount(DEFAULT_PROVINCIAL_RESERVED_COUNT);
        entity.setDistributedAt(null);
        entity.setDeleted(false);

        LeaderboardPrizeConfigEntity saved = leaderboardPrizeConfigRepository.save(entity);

        log.info("Created leaderboard prize config {} for week {} (lockAt={})",
                saved.getId(),
                saved.getWeekStartDate(),
                saved.getLockAt());

        return leaderboardMapper.toPrizeConfigResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PrizeConfigResponse> getPrizeConfigs(
            LocalDate weekStartDate,
            PrizeConfigStatus status,
            int page,
            int size) {

        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "weekStartDate"));

        Specification<LeaderboardPrizeConfigEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (weekStartDate != null) {
                predicates.add(cb.equal(root.get("weekStartDate"), weekStartDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LeaderboardPrizeConfigEntity> configPage = leaderboardPrizeConfigRepository.findAll(spec, pageable);

        List<PrizeConfigResponse> content = configPage.getContent().stream()
                .map(leaderboardMapper::toPrizeConfigResponse)
                .toList();

        return PagedResponse.of(
                content,
                configPage.getNumber(),
                configPage.getSize(),
                configPage.getTotalElements(),
                configPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PrizeConfigResponse getPrizeConfigById(String id) {
        LeaderboardPrizeConfigEntity entity = leaderboardPrizeConfigRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leaderboard prize config not found"));
        return leaderboardMapper.toPrizeConfigResponse(entity);
    }

    @Override
    @Transactional
    public void cancelPrizeConfig(String id) {
        LeaderboardPrizeConfigEntity config = leaderboardPrizeConfigRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leaderboard prize config not found"));

        if (config.getStatus() != PrizeConfigStatus.CONFIGURED) {
            throw new AppException("Only CONFIGURED prize config can be cancelled", HttpStatus.BAD_REQUEST);
        }

        releaseReservedStock(config);

        config.setStatus(PrizeConfigStatus.CANCELLED);
        config.setDeleted(true);
        leaderboardPrizeConfigRepository.save(config);

        log.info("Cancelled leaderboard prize config {} for week {}",
                config.getId(),
                config.getWeekStartDate());
    }

    @Override
    @Transactional(readOnly = true)
    public void finalizeDueWeeks() {
        LocalDateTime now = LocalDateTime.now();
        List<String> dueConfigIds = leaderboardPrizeConfigRepository.findIdsToFinalize(PrizeConfigStatus.CONFIGURED, now);

        for (String configId : dueConfigIds) {
            try {
                finalizeWeek(configId);
            } catch (Exception ex) {
                log.error("Failed to finalize leaderboard week for prize config {}", configId, ex);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void finalizeWeek(String prizeConfigId) {
        FinalizationContext context = runInTransaction(() -> prepareSnapshots(prizeConfigId));
        if (context.skipDistribution()) {
            return;
        }

        List<String> pendingSnapshotIds = leaderboardSnapshotRepository.findPendingSnapshotIds(prizeConfigId);

        int rewardedCount = 0;
        int failedCount = 0;

        for (String snapshotId : pendingSnapshotIds) {
            try {
                runInNewTransaction(() -> rewardSnapshot(snapshotId));
                rewardedCount++;
            } catch (Exception ex) {
                failedCount++;
                log.error("Failed to distribute leaderboard reward for snapshot {}", snapshotId, ex);
            }
        }

        runInTransaction(() -> completeWeekFinalization(prizeConfigId));
        deleteWeekKeys(context.weekStartDate());

        log.info("Completed leaderboard finalization for config {} with {} rewarded and {} failed",
                prizeConfigId,
                rewardedCount,
                failedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public void updateScore(String userId, BigDecimal weeklyPoints, LocalDateTime lastPointEarnedAt) {
        if (!hasText(userId) || weeklyPoints == null || lastPointEarnedAt == null) {
            return;
        }

        try {
            LocalDate effectiveWeekStart = resolveWeekStartForScore(lastPointEarnedAt);
            String nationalKey = buildNationalKey(effectiveWeekStart);
            double compositeScore = buildCompositeScore(weeklyPoints, lastPointEarnedAt);

            stringRedisTemplate.opsForZSet().add(nationalKey, userId, compositeScore);
            stringRedisTemplate.expire(nationalKey, LEADERBOARD_TTL);

            userProfileRepository.findByUserId(userId)
                    .map(UserProfileEntity::getProvince)
                    .map(this::normalizeProvince)
                    .filter(this::hasText)
                    .ifPresent(province -> {
                        String provinceKey = buildProvinceKey(province, effectiveWeekStart);
                        stringRedisTemplate.opsForZSet().add(provinceKey, userId, compositeScore);
                        stringRedisTemplate.expire(provinceKey, LEADERBOARD_TTL);
                    });
        } catch (Exception ex) {
            log.error("Failed to update leaderboard redis score for user {}", userId, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(LocalDate weekStartDate, LeaderboardScope scope, String province) {
        LocalDate resolvedWeekStart = weekStartDate != null
                ? weekStartDate
                : currentWeekStart(LocalDate.now());

        LeaderboardScope resolvedScope = scope != null ? scope : LeaderboardScope.NATIONAL;
        String normalizedProvince = normalizeProvince(province);
        validateScope(resolvedScope, normalizedProvince);

        LocalDate thisWeekStart = currentWeekStart(LocalDate.now());

        List<LeaderboardEntryResponse> entries = resolvedWeekStart.isBefore(thisWeekStart)
                ? getPastWeekLeaderboardEntries(resolvedWeekStart, resolvedScope, normalizedProvince)
                : getCurrentWeekLeaderboardEntries(resolvedWeekStart, resolvedScope, normalizedProvince);

        return LeaderboardResponse.builder()
                .weekStartDate(resolvedWeekStart)
                .scope(resolvedScope)
                .province(normalizedProvince)
                .entries(entries)
                .build();
    }

            @Override
            @Transactional(readOnly = true)
            public LeaderboardPrizeResponse getLeaderboardPrize(LocalDate weekStartDate) {
            LocalDate resolvedWeekStart = weekStartDate != null
                ? weekStartDate
                : currentWeekStart(LocalDate.now());

            LeaderboardPrizeConfigEntity config = leaderboardPrizeConfigRepository
                .findByWeekStartDateAndIsDeletedFalse(resolvedWeekStart)
                .orElseThrow(() -> new ResourceNotFoundException("Leaderboard prize config not found for requested week"));

            return LeaderboardPrizeResponse.builder()
                .prizeConfigId(config.getId())
                .weekStartDate(config.getWeekStartDate())
                .lockAt(config.getLockAt())
                .status(config.getStatus())
                .nationalReservedCount(config.getNationalReservedCount())
                .provincialReservedCount(config.getProvincialReservedCount())
                .distributedAt(config.getDistributedAt())
                .nationalVoucher(voucherMapper.toVoucherTemplateResponse(config.getNationalVoucherTemplate()))
                .provincialVoucher(voucherMapper.toVoucherTemplateResponse(config.getProvincialVoucherTemplate()))
                .build();
            }

    private FinalizationContext prepareSnapshots(String prizeConfigId) {
        LeaderboardPrizeConfigEntity config = leaderboardPrizeConfigRepository.findByIdForUpdate(prizeConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("Leaderboard prize config not found"));

        if (config.getStatus() == PrizeConfigStatus.CANCELLED) {
            throw new AppException("Cannot finalize a cancelled leaderboard prize config", HttpStatus.BAD_REQUEST);
        }

        if (config.getStatus() == PrizeConfigStatus.DISTRIBUTED) {
            log.info("Prize config {} already distributed; retrying any unrewarded snapshots only", prizeConfigId);
            return new FinalizationContext(config.getWeekStartDate(), false);
        }

        if (!leaderboardSnapshotRepository.existsByPrizeConfigId(prizeConfigId)) {
            createSnapshotsFromRedis(config);
        }

        log.info("Prepared snapshots for leaderboard finalization with config {}", prizeConfigId);
        return new FinalizationContext(config.getWeekStartDate(), false);
    }

    private void createSnapshotsFromRedis(LeaderboardPrizeConfigEntity config) {
        LocalDate weekStartDate = config.getWeekStartDate();

        List<RankingTuple> nationalRankings = fetchTopRankings(buildNationalKey(weekStartDate));

        List<String> provinces = userProfileRepository.findDistinctProvincesForLeaderboard();
        Map<String, List<RankingTuple>> provincialRankings = new HashMap<>();
        for (String province : provinces) {
            String normalizedProvince = normalizeProvince(province);
            if (!hasText(normalizedProvince)) {
                continue;
            }
            List<RankingTuple> rankings = fetchTopRankings(buildProvinceKey(normalizedProvince, weekStartDate));
            if (!rankings.isEmpty()) {
                provincialRankings.put(normalizedProvince, rankings);
            }
        }

        Set<String> userIds = new HashSet<>();
        nationalRankings.forEach(tuple -> userIds.add(tuple.userId()));
        provincialRankings.values().forEach(rankings -> rankings.forEach(tuple -> userIds.add(tuple.userId())));

        Map<String, PointWalletEntity> walletsByUserId = loadWalletsByUserId(userIds);
        Map<String, UserEntity> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (left, right) -> left));

        List<LeaderboardSnapshotEntity> snapshots = new ArrayList<>();
        appendSnapshots(
                snapshots,
                config,
                LeaderboardScope.NATIONAL,
                null,
                nationalRankings,
                usersById,
                walletsByUserId);

        for (Map.Entry<String, List<RankingTuple>> entry : provincialRankings.entrySet()) {
            appendSnapshots(
                    snapshots,
                    config,
                    LeaderboardScope.PROVINCIAL,
                    entry.getKey(),
                    entry.getValue(),
                    usersById,
                    walletsByUserId);
        }

        if (!snapshots.isEmpty()) {
            leaderboardSnapshotRepository.saveAll(snapshots);
            log.info("Saved {} leaderboard snapshots for config {}", snapshots.size(), config.getId());
        } else {
            log.info("No leaderboard snapshots generated for config {}", config.getId());
        }
    }

    private void appendSnapshots(
            List<LeaderboardSnapshotEntity> destination,
            LeaderboardPrizeConfigEntity config,
            LeaderboardScope scope,
            String province,
            List<RankingTuple> rankings,
            Map<String, UserEntity> usersById,
            Map<String, PointWalletEntity> walletsByUserId) {

        for (int index = 0; index < rankings.size(); index++) {
            RankingTuple tuple = rankings.get(index);
            UserEntity user = usersById.get(tuple.userId());

            if (user == null) {
                log.warn("Skip snapshot for missing user {}", tuple.userId());
                continue;
            }

            PointWalletEntity wallet = walletsByUserId.get(tuple.userId());
            BigDecimal weeklyPoints = wallet != null && wallet.getWeeklyPoints() != null
                    ? wallet.getWeeklyPoints()
                    : BigDecimal.ZERO;

            LeaderboardSnapshotEntity snapshot = LeaderboardSnapshotEntity.builder()
                    .prizeConfig(config)
                    .weekStartDate(config.getWeekStartDate())
                    .scope(scope)
                    .province(province)
                    .rank(index + 1)
                    .user(user)
                    .weeklyPoints(weeklyPoints)
                    .rewarded(false)
                    .build();

            destination.add(snapshot);
        }
    }

    private void rewardSnapshot(String snapshotId) {
        LeaderboardSnapshotEntity snapshot = leaderboardSnapshotRepository.findByIdForUpdate(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Leaderboard snapshot not found"));

        if (snapshot.isRewarded()) {
            return;
        }

        String voucherTemplateId = snapshot.getScope() == LeaderboardScope.NATIONAL
                ? snapshot.getPrizeConfig().getNationalVoucherTemplate().getId()
                : snapshot.getPrizeConfig().getProvincialVoucherTemplate().getId();

        UserVoucherEntity userVoucher = voucherService.grantVoucherToUser(
                snapshot.getUser().getId(),
                voucherTemplateId,
                VoucherSource.LEADERBOARD_REWARD);

        snapshot.setRewarded(true);
        snapshot.setUserVoucher(userVoucher);
        leaderboardSnapshotRepository.save(snapshot);

        log.info("Rewarded snapshot {} with userVoucher {}", snapshot.getId(), userVoucher.getId());
    }

    private void completeWeekFinalization(String prizeConfigId) {
        LeaderboardPrizeConfigEntity config = leaderboardPrizeConfigRepository.findByIdForUpdate(prizeConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("Leaderboard prize config not found"));

        if (config.getStatus() == PrizeConfigStatus.DISTRIBUTED) {
            return;
        }

        if (config.getStatus() == PrizeConfigStatus.CANCELLED) {
            throw new AppException("Cannot complete a cancelled leaderboard prize config", HttpStatus.BAD_REQUEST);
        }

        int resetCount = pointWalletRepository.resetWeeklyPointsForAllUsers();

        config.setStatus(PrizeConfigStatus.DISTRIBUTED);
        config.setDistributedAt(LocalDateTime.now());
        leaderboardPrizeConfigRepository.save(config);

        log.info("Marked config {} as DISTRIBUTED and reset {} point wallets", prizeConfigId, resetCount);
    }

    private void reserveStock(VoucherTemplateEntity nationalTemplate, VoucherTemplateEntity provincialTemplate) {
        if (nationalTemplate.getId().equals(provincialTemplate.getId())) {
            int required = DEFAULT_NATIONAL_RESERVED_COUNT + DEFAULT_PROVINCIAL_RESERVED_COUNT;
            reserveOneTemplate(nationalTemplate.getId(), required, "national+provincial");
            return;
        }

        reserveOneTemplate(nationalTemplate.getId(), DEFAULT_NATIONAL_RESERVED_COUNT, "national");
        reserveOneTemplate(provincialTemplate.getId(), DEFAULT_PROVINCIAL_RESERVED_COUNT, "provincial");
    }

    private void reserveOneTemplate(String voucherTemplateId, int count, String label) {
        if (count <= 0) {
            return;
        }

        int updated = voucherTemplateRepository.decreaseRemainingStockBatch(voucherTemplateId, count);
        if (updated == 0) {
            throw new AppException(
                    "Insufficient stock for " + label + " leaderboard voucher reservation",
                    HttpStatus.BAD_REQUEST);
        }

        log.info("Reserved {} vouchers from template {} for {} leaderboard prize", count, voucherTemplateId, label);
    }

    private void releaseReservedStock(LeaderboardPrizeConfigEntity config) {
        int nationalReserved = Optional.ofNullable(config.getNationalReservedCount())
                .orElse(DEFAULT_NATIONAL_RESERVED_COUNT);
        int provincialReserved = Optional.ofNullable(config.getProvincialReservedCount())
                .orElse(DEFAULT_PROVINCIAL_RESERVED_COUNT);

        String nationalTemplateId = config.getNationalVoucherTemplate().getId();
        String provincialTemplateId = config.getProvincialVoucherTemplate().getId();

        if (nationalTemplateId.equals(provincialTemplateId)) {
            VoucherTemplateEntity lockedTemplate = voucherTemplateRepository.findByIdForUpdate(nationalTemplateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));
            lockedTemplate.setRemainingStock(lockedTemplate.getRemainingStock() + nationalReserved + provincialReserved);
            voucherTemplateRepository.save(lockedTemplate);
            return;
        }

        VoucherTemplateEntity lockedNationalTemplate = voucherTemplateRepository.findByIdForUpdate(nationalTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));

        VoucherTemplateEntity lockedProvincialTemplate = voucherTemplateRepository.findByIdForUpdate(provincialTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));

        lockedNationalTemplate.setRemainingStock(lockedNationalTemplate.getRemainingStock() + nationalReserved);
        lockedProvincialTemplate.setRemainingStock(lockedProvincialTemplate.getRemainingStock() + provincialReserved);

        voucherTemplateRepository.save(lockedNationalTemplate);
        voucherTemplateRepository.save(lockedProvincialTemplate);
    }

    private LockedTemplates lockTemplates(String nationalVoucherTemplateId, String provincialVoucherTemplateId) {
        VoucherTemplateEntity nationalTemplate = voucherTemplateRepository.findByIdForUpdate(nationalVoucherTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("National voucher template not found"));

        VoucherTemplateEntity provincialTemplate;
        if (Objects.equals(nationalVoucherTemplateId, provincialVoucherTemplateId)) {
            provincialTemplate = nationalTemplate;
        } else {
            provincialTemplate = voucherTemplateRepository.findByIdForUpdate(provincialVoucherTemplateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Provincial voucher template not found"));
        }

        return new LockedTemplates(nationalTemplate, provincialTemplate);
    }

    private List<LeaderboardEntryResponse> getCurrentWeekLeaderboardEntries(
            LocalDate weekStartDate,
            LeaderboardScope scope,
            String province) {

        String key = scope == LeaderboardScope.NATIONAL
                ? buildNationalKey(weekStartDate)
                : buildProvinceKey(province, weekStartDate);

        List<RankingTuple> rankings = fetchTopRankings(key);
        if (rankings.isEmpty()) {
            return List.of();
        }

        List<String> orderedUserIds = rankings.stream().map(RankingTuple::userId).toList();

        Map<String, PointWalletEntity> walletsByUserId = loadWalletsByUserId(orderedUserIds);

        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (int index = 0; index < rankings.size(); index++) {
            RankingTuple tuple = rankings.get(index);
            PointWalletEntity wallet = walletsByUserId.get(tuple.userId());
            UserEntity user = wallet != null ? wallet.getUser() : null;

            entries.add(LeaderboardEntryResponse.builder()
                    .rank(index + 1)
                    .userId(tuple.userId())
                    .displayName(resolveDisplayName(user))
                    .avatarUrl(resolveAvatarUrl(user))
                    .province(resolveProvince(user, province))
                    .weeklyPoints(wallet != null && wallet.getWeeklyPoints() != null
                            ? wallet.getWeeklyPoints()
                            : BigDecimal.ZERO)
                    .build());
        }

        return entries;
    }

    private List<LeaderboardEntryResponse> getPastWeekLeaderboardEntries(
            LocalDate weekStartDate,
            LeaderboardScope scope,
            String province) {

        List<LeaderboardSnapshotEntity> snapshots = scope == LeaderboardScope.NATIONAL
                ? leaderboardSnapshotRepository.findByWeekStartDateAndScopeOrderByRankAsc(weekStartDate, scope)
                : leaderboardSnapshotRepository.findByWeekStartDateAndScopeAndProvinceOrderByRankAsc(
                        weekStartDate,
                        scope,
                        province);

        return snapshots.stream()
                .sorted(Comparator.comparing(LeaderboardSnapshotEntity::getRank))
                .map(snapshot -> LeaderboardEntryResponse.builder()
                        .rank(snapshot.getRank())
                        .userId(snapshot.getUser().getId())
                        .displayName(resolveDisplayName(snapshot.getUser()))
                        .avatarUrl(resolveAvatarUrl(snapshot.getUser()))
                        .province(resolveProvince(snapshot.getUser(), province))
                        .weeklyPoints(snapshot.getWeeklyPoints())
                        .build())
                .toList();
    }

    private List<RankingTuple> fetchTopRankings(String key) {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate
                    .opsForZSet()
                    .reverseRangeWithScores(key, 0, TOP_LIMIT - 1);

            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }

            return tuples.stream()
                    .filter(tuple -> tuple.getValue() != null)
                    .map(tuple -> new RankingTuple(
                            tuple.getValue(),
                            tuple.getScore() == null ? 0D : tuple.getScore()))
                    .toList();
        } catch (Exception ex) {
            log.error("Failed to fetch leaderboard rankings from redis key {}", key, ex);
            return List.of();
        }
    }

    private void deleteWeekKeys(LocalDate weekStartDate) {
        try {
            String dateSegment = weekStartDate.toString();
            String nationalKey = buildNationalKey(weekStartDate);
            String provincialPattern = PROVINCIAL_KEY_PREFIX + "*:" + dateSegment;

            Set<String> keysToDelete = new HashSet<>();
            keysToDelete.add(nationalKey);

            Set<String> provincialKeys = stringRedisTemplate.keys(provincialPattern);
            if (provincialKeys != null && !provincialKeys.isEmpty()) {
                keysToDelete.addAll(provincialKeys);
            }

            if (!keysToDelete.isEmpty()) {
                stringRedisTemplate.delete(keysToDelete);
            }
        } catch (Exception ex) {
            log.error("Failed to delete old leaderboard redis keys for week {}", weekStartDate, ex);
        }
    }

    private Map<String, PointWalletEntity> loadWalletsByUserId(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        return pointWalletRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(PointWalletEntity::getUserId, Function.identity(), (left, right) -> left));
    }

    private LocalDate resolveWeekStartForScore(LocalDateTime lastPointEarnedAt) {
        LocalDate candidateWeekStart = currentWeekStart(lastPointEarnedAt.toLocalDate());

        Optional<LeaderboardPrizeConfigEntity> configOptional = leaderboardPrizeConfigRepository
                .findByWeekStartDateAndIsDeletedFalse(candidateWeekStart);

        if (configOptional.isEmpty()) {
            return candidateWeekStart;
        }

        LeaderboardPrizeConfigEntity config = configOptional.get();
        if (config.getStatus() == PrizeConfigStatus.CANCELLED) {
            return candidateWeekStart;
        }

        if (!lastPointEarnedAt.isBefore(config.getLockAt())) {
            return candidateWeekStart.plusWeeks(1);
        }

        return candidateWeekStart;
    }

    private double buildCompositeScore(BigDecimal weeklyPoints, LocalDateTime lastPointEarnedAt) {
        long epochMilli = java.sql.Timestamp.valueOf(lastPointEarnedAt).getTime();
        double tieBreakerComponent = (Long.MAX_VALUE - epochMilli) / TIE_BREAK_DIVISOR;
        return weeklyPoints.doubleValue() * SCORE_MULTIPLIER + tieBreakerComponent;
    }

    private String buildNationalKey(LocalDate weekStartDate) {
        return NATIONAL_KEY_PREFIX + weekStartDate;
    }

    private String buildProvinceKey(String province, LocalDate weekStartDate) {
        return PROVINCIAL_KEY_PREFIX + province + ":" + weekStartDate;
    }

    private void validateScope(LeaderboardScope scope, String province) {
        if (scope == LeaderboardScope.PROVINCIAL && !hasText(province)) {
            throw new AppException("province is required when scope=PROVINCIAL", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeProvince(String province) {
        if (province == null) {
            return null;
        }
        String trimmed = province.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveDisplayName(UserEntity user) {
        if (user == null) {
            return "Unknown user";
        }

        if (user.getUserProfile() != null && hasText(user.getUserProfile().getDisplayName())) {
            return user.getUserProfile().getDisplayName();
        }

        if (hasText(user.getUsername())) {
            return user.getUsername();
        }

        if (hasText(user.getEmail())) {
            return user.getEmail();
        }

        return user.getId();
    }

    private String resolveAvatarUrl(UserEntity user) {
        if (user == null
                || user.getUserProfile() == null
                || user.getUserProfile().getAvatar() == null) {
            return null;
        }
        return user.getUserProfile().getAvatar().getImageUrl();
    }

    private String resolveProvince(UserEntity user, String fallbackProvince) {
        if (user == null || user.getUserProfile() == null) {
            return fallbackProvince;
        }
        return normalizeProvince(user.getUserProfile().getProvince());
    }

    private LocalDate parseWeekStartDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new AppException("weekStartDate must be ISO date (yyyy-MM-dd)", HttpStatus.BAD_REQUEST);
        }
    }

    private LocalDateTime parseLockAt(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new AppException("lockAt must be ISO datetime (yyyy-MM-dd'T'HH:mm:ss)", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateConfigWindow(LocalDate weekStartDate, LocalDateTime lockAt) {
        LocalDate today = LocalDate.now();

        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new AppException("weekStartDate must be a Monday", HttpStatus.BAD_REQUEST);
        }

        if (!weekStartDate.isAfter(today)) {
            throw new AppException("weekStartDate must be a future Monday", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime weekStartDateTime = weekStartDate.atStartOfDay();
        LocalDateTime nextMonday = weekStartDate.plusWeeks(1).atStartOfDay();

        if (lockAt.isBefore(weekStartDateTime)) {
            throw new AppException("lockAt must be on or after week start", HttpStatus.BAD_REQUEST);
        }

        if (!lockAt.isBefore(nextMonday)) {
            throw new AppException("lockAt must be before next Monday 00:00", HttpStatus.BAD_REQUEST);
        }
    }

    private LocalDate currentWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> T runInTransaction(Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> action.get());
    }

    private void runInTransaction(Runnable action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private void runInNewTransaction(Runnable action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private record RankingTuple(String userId, double compositeScore) {
    }

    private record LockedTemplates(VoucherTemplateEntity nationalTemplate, VoucherTemplateEntity provincialTemplate) {
    }

    private record FinalizationContext(LocalDate weekStartDate, boolean skipDistribution) {
    }
}
