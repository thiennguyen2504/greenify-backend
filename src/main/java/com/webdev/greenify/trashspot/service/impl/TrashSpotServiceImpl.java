package com.webdev.greenify.trashspot.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.service.PointService;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.station.repository.WasteTypeRepository;
import com.webdev.greenify.trashspot.dto.request.CreateResolveRequestRequest;
import com.webdev.greenify.trashspot.dto.request.CreateTrashSpotRequest;
import com.webdev.greenify.trashspot.dto.request.ReviewResolveRequest;
import com.webdev.greenify.trashspot.dto.request.SubmitVerificationRequest;
import com.webdev.greenify.trashspot.dto.response.ResolveRequestResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotDetailResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotSummaryResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotVerificationResponse;
import com.webdev.greenify.trashspot.entity.TrashSpotEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotImageEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotResolveImageEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotResolveRequestEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotVerificationEntity;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import com.webdev.greenify.trashspot.enumeration.SeverityTier;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;
import com.webdev.greenify.trashspot.mapper.TrashSpotMapper;
import com.webdev.greenify.trashspot.repository.TrashSpotRepository;
import com.webdev.greenify.trashspot.repository.TrashSpotResolveRequestRepository;
import com.webdev.greenify.trashspot.repository.TrashSpotVerificationRepository;
import com.webdev.greenify.trashspot.service.TrashSpotService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrashSpotServiceImpl implements TrashSpotService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final double DUPLICATE_RADIUS_METERS = 200.0;
    private static final int REOPEN_RESOLVED_WINDOW_DAYS = 30;
    private static final int VERIFY_THRESHOLD = 3;

    private static final List<String> REPORTER_ACTION_NAMES = List.of(
            "Báo cáo môi trường",
            "Report illegal dumping/polluted spots");

    private static final List<String> REVIEWER_ACTION_NAMES = List.of(
            "Duyệt bài hợp lệ với tư cách CTV",
            "Review posts as a Contributor");

    private final TrashSpotRepository trashSpotRepository;
    private final TrashSpotVerificationRepository trashSpotVerificationRepository;
    private final TrashSpotResolveRequestRepository trashSpotResolveRequestRepository;
    private final WasteTypeRepository wasteTypeRepository;
    private final UserRepository userRepository;
    private final TrashSpotMapper trashSpotMapper;
    private final PointService pointService;
    private final GreenActionTypeRepository greenActionTypeRepository;

    private volatile GreenActionTypeEntity reporterActionTypeCache;
    private volatile GreenActionTypeEntity reviewerActionTypeCache;

    @Override
    @Transactional
    public CreateOrMergeResult createOrMerge(CreateTrashSpotRequest request) {
        UserEntity reporter = getCurrentUser();
        Set<WasteTypeEntity> wasteTypes = resolveWasteTypes(request.getWasteTypeIds());

        OptionalNearbySpots nearbySpots = findNearbySpots(request.getLatitude(), request.getLongitude());

        if (nearbySpots.nonResolvedSpotId() != null) {
            TrashSpotEntity mergedSpot = getTrashSpotForWrite(nearbySpots.nonResolvedSpotId());
            appendDescription(mergedSpot, request.getDescription());
            mergedSpot.getWasteTypes().addAll(wasteTypes);
            addSpotImages(mergedSpot, request.getImages(), reporter);
            recalculateHotScore(mergedSpot);
            mergedSpot = trashSpotRepository.save(mergedSpot);

            awardReporterPoints(reporter, mergedSpot);
            log.info("Merged report into existing trash spot {} by reporter {}", mergedSpot.getId(), reporter.getId());

            return new CreateOrMergeResult(toDetailResponse(mergedSpot), false);
        }

        if (nearbySpots.resolvedSpotId() != null) {
            TrashSpotEntity resolvedSpot = getTrashSpotForWrite(nearbySpots.resolvedSpotId());
            if (isResolvedWithinWindow(resolvedSpot)) {
                resolvedSpot.setStatus(TrashSpotStatus.PENDING_VERIFY);
                resolvedSpot.setAssignedNgo(null);
                resolvedSpot.setClaimedAt(null);
                resolvedSpot.setResolvedAt(null);
                resolvedSpot.setVerificationCount(1);

                trashSpotVerificationRepository.deleteByTrashSpotId(resolvedSpot.getId());

                appendDescription(resolvedSpot, request.getDescription());
                resolvedSpot.getWasteTypes().addAll(wasteTypes);
                addSpotImages(resolvedSpot, request.getImages(), reporter);
                recalculateHotScore(resolvedSpot);
                resolvedSpot = trashSpotRepository.save(resolvedSpot);

                awardReporterPoints(reporter, resolvedSpot);
                log.info("Reopened resolved trash spot {} due to new nearby report by {}", resolvedSpot.getId(), reporter.getId());

                return new CreateOrMergeResult(toDetailResponse(resolvedSpot), false);
            }
        }

        TrashSpotEntity newSpot = TrashSpotEntity.builder()
                .reporter(reporter)
                .description(normalizeDescription(request.getDescription()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .province(request.getProvince())
                .status(TrashSpotStatus.PENDING_VERIFY)
                .verificationCount(0)
                .build();
        newSpot.getWasteTypes().addAll(wasteTypes);
        addSpotImages(newSpot, request.getImages(), reporter);
        recalculateHotScore(newSpot);

        newSpot = trashSpotRepository.save(newSpot);
        awardReporterPoints(reporter, newSpot);
        log.info("Created new trash spot {} by reporter {}", newSpot.getId(), reporter.getId());

        return new CreateOrMergeResult(toDetailResponse(newSpot), true);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TrashSpotSummaryResponse> getTrashSpots(
            String province,
            TrashSpotStatus status,
            int page,
            int size) {

        Pageable pageable = buildDefaultPageable(page, size);
        Specification<TrashSpotEntity> specification = baseSpecification()
                .and(hasProvince(province));

        if (status != null) {
            specification = specification.and(hasStatus(status));
        } else {
            specification = specification.and(notStatus(TrashSpotStatus.RESOLVED));
        }

        Page<TrashSpotEntity> spotsPage = trashSpotRepository.findAll(specification, pageable);
        List<TrashSpotSummaryResponse> content = spotsPage.getContent().stream()
                .map(trashSpotMapper::toSummaryResponse)
                .toList();

        return PagedResponse.of(
                content,
                spotsPage.getNumber(),
                spotsPage.getSize(),
                spotsPage.getTotalElements(),
                spotsPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public TrashSpotDetailResponse getTrashSpotDetail(String id) {
        TrashSpotEntity spot = getTrashSpotOrThrow(id);
        return toDetailResponse(spot);
    }

    @Override
    @Transactional
    public TrashSpotVerificationResponse submitVerification(String id, SubmitVerificationRequest request) {
        TrashSpotEntity spot = getTrashSpotOrThrow(id);

        if (spot.getStatus() != TrashSpotStatus.PENDING_VERIFY) {
            throw new AppException("Điểm rác không ở trạng thái chờ xác minh", HttpStatus.BAD_REQUEST);
        }

        String currentUserId = getCurrentUserId();
        if (spot.getReporter().getId().equals(currentUserId)) {
            throw new AppException("Không thể xác minh báo cáo của chính mình", HttpStatus.FORBIDDEN);
        }

        if (trashSpotVerificationRepository.existsByTrashSpotIdAndVerifierIdAndIsDeletedFalse(spot.getId(), currentUserId)) {
            throw new AppException("이미 확인하셨습니다", HttpStatus.CONFLICT);
        }

        UserEntity verifier = getCurrentUser();
        SubmitVerificationRequest effectiveRequest = request != null
                ? request
                : SubmitVerificationRequest.builder().build();

        TrashSpotVerificationEntity verification = TrashSpotVerificationEntity.builder()
                .trashSpot(spot)
                .verifier(verifier)
                .note(effectiveRequest.getNote())
                .build();

        try {
            verification = trashSpotVerificationRepository.saveAndFlush(verification);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException("이미 확인하셨습니다", HttpStatus.CONFLICT);
        }

        int currentVerificationCount = spot.getVerificationCount() != null ? spot.getVerificationCount() : 0;
        int updatedVerificationCount = currentVerificationCount + 1;
        spot.setVerificationCount(updatedVerificationCount);

        if (updatedVerificationCount >= VERIFY_THRESHOLD) {
            spot.setStatus(TrashSpotStatus.VERIFIED);
            log.info("Trash spot {} reached verification threshold and moved to VERIFIED", spot.getId());
        }

        recalculateHotScore(spot);
        trashSpotRepository.save(spot);
        awardVerifierPoints(verifier, verification.getId(), spot);

        log.info("Verification {} submitted by {} for trash spot {}", verification.getId(), verifier.getId(), spot.getId());
        return trashSpotMapper.toVerificationResponse(verification);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TrashSpotSummaryResponse> getNgoTrashSpots(String province, int page, int size) {
        Pageable pageable = buildDefaultPageable(page, size);
        Specification<TrashSpotEntity> specification = baseSpecification()
                .and(hasProvince(province))
                .and(statusIn(Set.of(
                        TrashSpotStatus.VERIFIED,
                        TrashSpotStatus.REOPENED,
                        TrashSpotStatus.IN_PROGRESS)));

        Page<TrashSpotEntity> spotsPage = trashSpotRepository.findAll(specification, pageable);
        List<TrashSpotSummaryResponse> content = spotsPage.getContent().stream()
                .map(trashSpotMapper::toSummaryResponse)
                .toList();

        return PagedResponse.of(
                content,
                spotsPage.getNumber(),
                spotsPage.getSize(),
                spotsPage.getTotalElements(),
                spotsPage.getTotalPages());
    }

    @Override
    @Transactional
    public TrashSpotDetailResponse claimSpot(String id) {
        UserEntity ngo = getCurrentUser();
        TrashSpotEntity spot = trashSpotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trash spot not found"));

        if (spot.getStatus() == TrashSpotStatus.IN_PROGRESS || spot.getStatus() == TrashSpotStatus.RESOLVED) {
            throw new AppException("Điểm rác đã được nhận hoặc đã xử lý", HttpStatus.CONFLICT);
        }

        if (spot.getStatus() != TrashSpotStatus.VERIFIED && spot.getStatus() != TrashSpotStatus.REOPENED) {
            throw new AppException("Chỉ có thể nhận điểm rác đã được xác minh hoặc mở lại", HttpStatus.BAD_REQUEST);
        }

        spot.setStatus(TrashSpotStatus.IN_PROGRESS);
        spot.setAssignedNgo(ngo);
        spot.setClaimedAt(LocalDateTime.now());
        trashSpotRepository.save(spot);

        log.info("NGO {} claimed trash spot {}", ngo.getId(), spot.getId());
        return toDetailResponse(getTrashSpotOrThrow(spot.getId()));
    }

    @Override
    @Transactional
    public ResolveRequestResponse createResolveRequest(String id, CreateResolveRequestRequest request) {
        UserEntity ngo = getCurrentUser();
        TrashSpotEntity spot = getTrashSpotOrThrow(id);

        if (spot.getStatus() != TrashSpotStatus.IN_PROGRESS) {
            throw new AppException("Điểm rác chưa ở trạng thái đang xử lý", HttpStatus.BAD_REQUEST);
        }

        if (spot.getAssignedNgo() == null || !spot.getAssignedNgo().getId().equals(ngo.getId())) {
            throw new AppException("Chỉ NGO đã nhận xử lý mới được gửi yêu cầu hoàn thành", HttpStatus.FORBIDDEN);
        }

        TrashSpotResolveRequestEntity resolveRequest = TrashSpotResolveRequestEntity.builder()
                .trashSpot(spot)
                .ngo(ngo)
                .description(normalizeDescription(request.getDescription()))
                .cleanedAt(request.getCleanedAt())
                .status(ResolveRequestStatus.PENDING_ADMIN_REVIEW)
                .build();

        addResolveImages(resolveRequest, request.getImages());
        resolveRequest = trashSpotResolveRequestRepository.save(resolveRequest);

        log.info("Resolve request {} created for trash spot {} by NGO {}",
                resolveRequest.getId(),
                spot.getId(),
                ngo.getId());

        return trashSpotMapper.toResolveRequestResponse(resolveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TrashSpotSummaryResponse> getAdminTrashSpots(
            TrashSpotStatus status,
            String province,
            int page,
            int size) {

        Pageable pageable = buildDefaultPageable(page, size);
        Specification<TrashSpotEntity> specification = baseSpecification()
                .and(hasProvince(province));

        if (status != null) {
            specification = specification.and(hasStatus(status));
        }

        Page<TrashSpotEntity> spotsPage = trashSpotRepository.findAll(specification, pageable);
        List<TrashSpotSummaryResponse> content = spotsPage.getContent().stream()
                .map(trashSpotMapper::toSummaryResponse)
                .toList();

        return PagedResponse.of(
                content,
                spotsPage.getNumber(),
                spotsPage.getSize(),
                spotsPage.getTotalElements(),
                spotsPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ResolveRequestResponse> getResolveRequests(ResolveRequestStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                clampPageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<TrashSpotResolveRequestEntity> resolveRequests = status != null
                ? trashSpotResolveRequestRepository.findByStatusAndIsDeletedFalse(status, pageable)
                : trashSpotResolveRequestRepository.findByIsDeletedFalse(pageable);

        List<ResolveRequestResponse> content = resolveRequests.getContent().stream()
                .map(trashSpotMapper::toResolveRequestResponse)
                .toList();

        return PagedResponse.of(
                content,
                resolveRequests.getNumber(),
                resolveRequests.getSize(),
                resolveRequests.getTotalElements(),
                resolveRequests.getTotalPages());
    }

    @Override
    @Transactional
    public ResolveRequestResponse approveResolveRequest(String resolveRequestId) {
        UserEntity admin = getCurrentUser();

        TrashSpotResolveRequestEntity resolveRequest = trashSpotResolveRequestRepository.findByIdAndIsDeletedFalse(resolveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Resolve request not found"));

        if (resolveRequest.getStatus() != ResolveRequestStatus.PENDING_ADMIN_REVIEW) {
            throw new AppException("Yêu cầu đã được xử lý trước đó", HttpStatus.BAD_REQUEST);
        }

        TrashSpotEntity spot = resolveRequest.getTrashSpot();
        spot.setStatus(TrashSpotStatus.RESOLVED);
        spot.setResolvedAt(LocalDateTime.now());
        trashSpotRepository.save(spot);

        resolveRequest.setStatus(ResolveRequestStatus.APPROVED);
        resolveRequest.setRejectReason(null);
        resolveRequest.setReviewedBy(admin);
        resolveRequest.setReviewedAt(LocalDateTime.now());
        resolveRequest = trashSpotResolveRequestRepository.save(resolveRequest);

        log.info("Admin {} approved resolve request {} for trash spot {}",
                admin.getId(),
                resolveRequest.getId(),
                spot.getId());

        return trashSpotMapper.toResolveRequestResponse(resolveRequest);
    }

    @Override
    @Transactional
    public ResolveRequestResponse rejectResolveRequest(String resolveRequestId, ReviewResolveRequest request) {
        UserEntity admin = getCurrentUser();

        TrashSpotResolveRequestEntity resolveRequest = trashSpotResolveRequestRepository.findByIdAndIsDeletedFalse(resolveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Resolve request not found"));

        if (resolveRequest.getStatus() != ResolveRequestStatus.PENDING_ADMIN_REVIEW) {
            throw new AppException("Yêu cầu đã được xử lý trước đó", HttpStatus.BAD_REQUEST);
        }

        TrashSpotEntity spot = resolveRequest.getTrashSpot();
        // Keep spot in progress so the assigned NGO can submit a new resolve request with updated evidence.
        spot.setStatus(TrashSpotStatus.IN_PROGRESS);
        spot.setResolvedAt(null);
        trashSpotRepository.save(spot);

        resolveRequest.setStatus(ResolveRequestStatus.REJECTED);
        resolveRequest.setRejectReason(request.getRejectReason());
        resolveRequest.setReviewedBy(admin);
        resolveRequest.setReviewedAt(LocalDateTime.now());
        resolveRequest = trashSpotResolveRequestRepository.save(resolveRequest);

        log.info("Admin {} rejected resolve request {} for trash spot {}",
                admin.getId(),
                resolveRequest.getId(),
                spot.getId());

        return trashSpotMapper.toResolveRequestResponse(resolveRequest);
    }

    @Override
    @Transactional
    public TrashSpotDetailResponse reopenResolvedSpot(String id) {
        TrashSpotEntity spot = getTrashSpotForWrite(id);

        if (spot.getStatus() != TrashSpotStatus.RESOLVED) {
            throw new AppException("Chỉ có thể mở lại điểm rác đã xử lý", HttpStatus.BAD_REQUEST);
        }

        spot.setStatus(TrashSpotStatus.REOPENED);
        spot.setAssignedNgo(null);
        spot.setClaimedAt(null);
        spot.setResolvedAt(null);
        recalculateHotScore(spot);
        trashSpotRepository.save(spot);

        log.info("Trash spot {} manually reopened by admin", spot.getId());
        return toDetailResponse(getTrashSpotOrThrow(spot.getId()));
    }

    @Override
    @Transactional
    public int recalculateAllActiveHotScores() {
        List<TrashSpotEntity> spots = trashSpotRepository.findActiveForHotScoreRecalculation(Set.of(
                TrashSpotStatus.PENDING_VERIFY,
                TrashSpotStatus.VERIFIED,
                TrashSpotStatus.REOPENED));

        int updatedCount = 0;
        for (TrashSpotEntity spot : spots) {
            BigDecimal previousHotScore = spot.getHotScore();
            SeverityTier previousSeverityTier = spot.getSeverityTier();

            recalculateHotScore(spot);
            if (!Objects.equals(previousHotScore, spot.getHotScore())
                    || previousSeverityTier != spot.getSeverityTier()) {
                updatedCount++;
            }
        }

        if (!spots.isEmpty()) {
            trashSpotRepository.saveAll(spots);
        }

        log.info("Recalculated hot score for {} active trash spots ({} changed)", spots.size(), updatedCount);
        return updatedCount;
    }

    private TrashSpotDetailResponse toDetailResponse(TrashSpotEntity spot) {
        TrashSpotDetailResponse response = trashSpotMapper.toDetailResponse(spot);

        List<TrashSpotVerificationEntity> verifications = trashSpotVerificationRepository
                .findByTrashSpotIdAndIsDeletedFalseOrderByCreatedAtDesc(spot.getId());
        response.setVerifications(trashSpotMapper.toVerificationResponseList(verifications));

        List<TrashSpotResolveRequestEntity> resolveRequests = trashSpotResolveRequestRepository
                .findByTrashSpotIdAndIsDeletedFalseOrderByCreatedAtDesc(spot.getId());
        response.setResolveRequests(trashSpotMapper.toResolveRequestResponseList(resolveRequests));

        return response;
    }

    private OptionalNearbySpots findNearbySpots(BigDecimal latitude, BigDecimal longitude) {
        String resolvedStatus = TrashSpotStatus.RESOLVED.name();

        String nonResolvedSpotId = trashSpotRepository.findNearestNonResolvedWithinRadius(
                        latitude,
                        longitude,
                        DUPLICATE_RADIUS_METERS,
                        resolvedStatus)
                .map(TrashSpotEntity::getId)
                .orElse(null);

        String resolvedSpotId = trashSpotRepository.findNearestResolvedWithinRadius(
                        latitude,
                        longitude,
                        DUPLICATE_RADIUS_METERS,
                        resolvedStatus)
                .map(TrashSpotEntity::getId)
                .orElse(null);

        return new OptionalNearbySpots(nonResolvedSpotId, resolvedSpotId);
    }

    private boolean isResolvedWithinWindow(TrashSpotEntity spot) {
        if (spot.getResolvedAt() == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(REOPEN_RESOLVED_WINDOW_DAYS);
        return !spot.getResolvedAt().isBefore(threshold);
    }

    private void awardReporterPoints(UserEntity reporter, TrashSpotEntity spot) {
        GreenActionTypeEntity reporterActionType = getReporterActionType();
        BigDecimal points = reporterActionType.getSuggestedPoints();

        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        GreenActionPostEntity pointSourcePost = GreenActionPostEntity.builder()
                .id(spot.getId())
                .actionType(reporterActionType)
                .build();

        pointService.awardPointsToPostAuthor(reporter, pointSourcePost, points);
        log.info("Awarded {} reporter points to user {} for trash spot {}", points, reporter.getId(), spot.getId());
    }

    private void awardVerifierPoints(UserEntity verifier, String verificationId, TrashSpotEntity spot) {
        GreenActionTypeEntity reviewerActionType = getReviewerActionType();

        GreenActionPostEntity pointSourcePost = GreenActionPostEntity.builder()
                .id(spot.getId())
                .actionType(reviewerActionType)
                .build();

        pointService.awardPointsToReviewer(verifier, verificationId, pointSourcePost);
        log.info("Awarded verifier points to user {} for trash spot verification {}",
                verifier.getId(),
                verificationId);
    }

    private GreenActionTypeEntity getReporterActionType() {
        GreenActionTypeEntity cached = reporterActionTypeCache;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (reporterActionTypeCache == null) {
                reporterActionTypeCache = resolveActionTypeOrThrow(REPORTER_ACTION_NAMES, "báo cáo môi trường");
            }
            return reporterActionTypeCache;
        }
    }

    private GreenActionTypeEntity getReviewerActionType() {
        GreenActionTypeEntity cached = reviewerActionTypeCache;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (reviewerActionTypeCache == null) {
                reviewerActionTypeCache = resolveActionTypeOrThrow(REVIEWER_ACTION_NAMES, "duyệt xác minh");
            }
            return reviewerActionTypeCache;
        }
    }

    private GreenActionTypeEntity resolveActionTypeOrThrow(List<String> candidateNames, String purpose) {
        for (String actionName : candidateNames) {
            if (actionName == null || actionName.isBlank()) {
                continue;
            }
            GreenActionTypeEntity actionType = greenActionTypeRepository
                    .findFirstByActionNameIgnoreCaseAndIsActiveTrue(actionName)
                    .orElse(null);
            if (actionType != null) {
                return actionType;
            }
        }

        throw new AppException("Không tìm thấy cấu hình điểm cho " + purpose, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void recalculateHotScore(TrashSpotEntity spot) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = spot.getCreatedAt() != null ? spot.getCreatedAt() : now;

        double ageInHours = ChronoUnit.HOURS.between(createdAt, now);
        double decayFactor = 1.0 / (1.0 + ageInHours / 48.0);
        int verificationCount = spot.getVerificationCount() != null ? spot.getVerificationCount() : 0;
        double hotScore = (verificationCount * 3.0 + 1.0) * decayFactor;

        spot.setHotScore(BigDecimal.valueOf(hotScore).setScale(4, RoundingMode.HALF_UP));
        spot.setSeverityTier(resolveSeverityTier(hotScore));
    }

    private SeverityTier resolveSeverityTier(double hotScore) {
        if (hotScore >= 10.0) {
            return SeverityTier.SEVERITY_HIGH;
        }
        if (hotScore >= 5.0) {
            return SeverityTier.SEVERITY_MEDIUM;
        }
        return SeverityTier.SEVERITY_LOW;
    }

    private void addSpotImages(TrashSpotEntity spot, List<ImageRequestDTO> images, UserEntity uploadedBy) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (ImageRequestDTO imageRequest : images) {
            if (imageRequest == null) {
                throw new AppException("Thông tin ảnh không hợp lệ", HttpStatus.BAD_REQUEST);
            }
            TrashSpotImageEntity image = TrashSpotImageEntity.builder()
                    .trashSpot(spot)
                    .bucketName(imageRequest.getBucketName())
                    .objectKey(imageRequest.getObjectKey())
                    .imageUrl(imageRequest.getImageUrl())
                    .uploadedBy(uploadedBy)
                    .build();
            spot.getImages().add(image);
        }
    }

    private void addResolveImages(TrashSpotResolveRequestEntity resolveRequest, List<ImageRequestDTO> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (ImageRequestDTO imageRequest : images) {
            if (imageRequest == null) {
                throw new AppException("Thông tin ảnh không hợp lệ", HttpStatus.BAD_REQUEST);
            }
            TrashSpotResolveImageEntity image = TrashSpotResolveImageEntity.builder()
                    .resolveRequest(resolveRequest)
                    .bucketName(imageRequest.getBucketName())
                    .objectKey(imageRequest.getObjectKey())
                    .imageUrl(imageRequest.getImageUrl())
                    .build();
            resolveRequest.getImages().add(image);
        }
    }

    private Set<WasteTypeEntity> resolveWasteTypes(List<String> wasteTypeIds) {
        if (wasteTypeIds == null || wasteTypeIds.isEmpty()) {
            throw new AppException("Ít nhất một loại rác là bắt buộc", HttpStatus.BAD_REQUEST);
        }

        LinkedHashSet<String> distinctWasteTypeIds = wasteTypeIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<WasteTypeEntity> wasteTypes = wasteTypeRepository.findAllById(distinctWasteTypeIds);
        if (wasteTypes.size() != distinctWasteTypeIds.size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều loại rác không tồn tại");
        }

        return new LinkedHashSet<>(wasteTypes);
    }

    private void appendDescription(TrashSpotEntity spot, String newDescription) {
        String normalizedNewDescription = normalizeDescription(newDescription);
        if (normalizedNewDescription == null || normalizedNewDescription.isBlank()) {
            return;
        }

        if (spot.getDescription() == null || spot.getDescription().isBlank()) {
            spot.setDescription(normalizedNewDescription);
            return;
        }

        spot.setDescription(spot.getDescription() + "\n---\n" + normalizedNewDescription);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        return description.trim();
    }

    private TrashSpotEntity getTrashSpotOrThrow(String id) {
        return trashSpotRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trash spot not found"));
    }

    private TrashSpotEntity getTrashSpotForWrite(String id) {
        return trashSpotRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trash spot not found"));
    }

    private Specification<TrashSpotEntity> baseSpecification() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    private Specification<TrashSpotEntity> hasProvince(String province) {
        if (province == null || province.isBlank()) {
            return null;
        }
        String normalizedProvince = province.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("province")), normalizedProvince);
    }

    private Specification<TrashSpotEntity> hasStatus(TrashSpotStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<TrashSpotEntity> notStatus(TrashSpotStatus status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }

    private Specification<TrashSpotEntity> statusIn(Collection<TrashSpotStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    private Pageable buildDefaultPageable(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                clampPageSize(size),
                Sort.by(Sort.Direction.DESC, "hotScore").and(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private UserEntity getCurrentUser() {
        String currentUserId = getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private record OptionalNearbySpots(String nonResolvedSpotId, String resolvedSpotId) {
    }
}
