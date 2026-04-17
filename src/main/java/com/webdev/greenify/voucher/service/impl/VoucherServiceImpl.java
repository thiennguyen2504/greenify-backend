package com.webdev.greenify.voucher.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.InsufficientPointsException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.common.exception.VoucherExpiredException;
import com.webdev.greenify.common.exception.VoucherOutOfStockException;
import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.file.service.FileService;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.notification.service.EmailService;
import com.webdev.greenify.point.entity.PointLedgerEntity;
import com.webdev.greenify.point.entity.PointWalletEntity;
import com.webdev.greenify.point.enumeration.PointLedgerSourceType;
import com.webdev.greenify.point.enumeration.PointLedgerStatus;
import com.webdev.greenify.point.repository.PointLedgerRepository;
import com.webdev.greenify.point.repository.PointWalletRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.dto.request.CreateVoucherTemplateRequest;
import com.webdev.greenify.voucher.dto.request.ExchangeVoucherRequest;
import com.webdev.greenify.voucher.dto.request.UpdateVoucherTemplateRequest;
import com.webdev.greenify.voucher.dto.response.UserVoucherResponse;
import com.webdev.greenify.voucher.dto.response.VoucherMarketplaceResponse;
import com.webdev.greenify.voucher.dto.response.VoucherTemplateResponse;
import com.webdev.greenify.voucher.entity.UserVoucherEntity;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import com.webdev.greenify.voucher.enumeration.UserVoucherStatus;
import com.webdev.greenify.voucher.enumeration.VoucherSource;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;
import com.webdev.greenify.voucher.mapper.VoucherMapper;
import com.webdev.greenify.voucher.repository.UserVoucherRepository;
import com.webdev.greenify.voucher.repository.VoucherTemplateRepository;
import com.webdev.greenify.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final BigDecimal ZERO_POINTS = BigDecimal.ZERO;
    private static final String DEFAULT_WALLET_STATUS = "ACTIVE";
    private static final int VOUCHER_CODE_MAX_RETRY = 5;
    private static final int MAX_ACTION_DESCRIPTION_LENGTH = 200;

    private final VoucherTemplateRepository voucherTemplateRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final EmailService emailService;
    private final VoucherMapper voucherMapper;

    /**
     * Return vouchers that are ACTIVE, in-stock and not expired.
     * Optional required point range is applied directly in DB query for pagination correctness.
     */
    @Override
    @Transactional(readOnly = true)
    public VoucherMarketplaceResponse getAvailableVouchers(
            BigDecimal minRequiredPoints,
            BigDecimal maxRequiredPoints,
            int page,
            int size) {
        String userId = getCurrentUserId();
        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(
                effectivePage,
                effectiveSize,
                Sort.by(Sort.Direction.ASC, "requiredPoints").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        LocalDateTime now = LocalDateTime.now();

        Page<VoucherTemplateEntity> vouchersPage;
        if (minRequiredPoints == null && maxRequiredPoints == null) {
            vouchersPage = voucherTemplateRepository.findByStatusAndRemainingStockGreaterThanAndValidUntilAfter(
                    VoucherTemplateStatus.ACTIVE,
                    0,
                    now,
                    pageable);
        } else {
            vouchersPage = voucherTemplateRepository.findMarketplaceVouchersByRequiredPoints(
                    VoucherTemplateStatus.ACTIVE,
                    now,
                    minRequiredPoints,
                    maxRequiredPoints,
                    pageable);
        }

        BigDecimal availablePoints = pointWalletRepository.findByUserId(userId)
                .map(PointWalletEntity::getAvailablePoints)
            .orElseGet(() -> pointTransactionRepository.sumAvailablePointsByUserId(userId));

        List<VoucherTemplateResponse> content = vouchersPage.getContent().stream()
                .map(voucherMapper::toVoucherTemplateResponse)
                .toList();

        return VoucherMarketplaceResponse.of(
                availablePoints,
                content,
                vouchersPage.getNumber(),
                vouchersPage.getSize(),
                vouchersPage.getTotalElements(),
                vouchersPage.getTotalPages());
    }

    /**
     * Exchange operation is intentionally non-idempotent.
     * Frontend should show a confirmation modal and only submit once per user intent.
     *
     * Business rules enforced atomically:
     * 1) Deduct available points from point_wallets
     * 2) Decrease voucher stock with pessimistic lock
     * 3) Issue a unique voucher code
     * 4) Persist user voucher
     * 5) Persist immutable point ledger deduction
     */
    @Override
    @Transactional
    public UserVoucherResponse exchangeVoucher(ExchangeVoucherRequest request) {
        if (request == null || request.getVoucherTemplateId() == null || request.getVoucherTemplateId().isBlank()) {
            throw new AppException("Voucher template ID is required", HttpStatus.BAD_REQUEST);
        }

        String userId = getCurrentUserId();
        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PointWalletEntity wallet = pointWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> createDefaultWallet(currentUser));

        // Read-before-lock to fail fast on unaffordable requests.
        VoucherTemplateEntity preCheckTemplate = voucherTemplateRepository.findById(request.getVoucherTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));

        if (wallet.getAvailablePoints().compareTo(preCheckTemplate.getRequiredPoints()) < 0) {
            throw new InsufficientPointsException("Insufficient points to redeem this voucher");
        }

        // Lock only at the stock mutation stage to keep lock scope narrow.
        VoucherTemplateEntity lockedTemplate = voucherTemplateRepository
                .findByIdForUpdate(request.getVoucherTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));

        validateVoucherTemplateForExchange(lockedTemplate);

        if (wallet.getAvailablePoints().compareTo(lockedTemplate.getRequiredPoints()) < 0) {
            throw new InsufficientPointsException("Insufficient points to redeem this voucher");
        }

        wallet.setAvailablePoints(wallet.getAvailablePoints().subtract(lockedTemplate.getRequiredPoints()));
        pointWalletRepository.save(wallet);

        int updatedRemainingStock = lockedTemplate.getRemainingStock() - 1;
        lockedTemplate.setRemainingStock(updatedRemainingStock);
        if (updatedRemainingStock <= 0) {
            lockedTemplate.setStatus(VoucherTemplateStatus.DEPLETED);
        }
        voucherTemplateRepository.save(lockedTemplate);

        UserVoucherEntity userVoucher = UserVoucherEntity.builder()
                .user(currentUser)
                .voucherTemplate(lockedTemplate)
                .voucherCode(generateUniqueVoucherCode())
                .source(VoucherSource.REDEEM)
                .status(UserVoucherStatus.AVAILABLE)
                .expiresAt(lockedTemplate.getValidUntil())
                .build();
        userVoucher = userVoucherRepository.save(userVoucher);

        PointLedgerEntity ledgerEntry = PointLedgerEntity.builder()
                .user(currentUser)
                .amount(lockedTemplate.getRequiredPoints().negate())
                .sourceType(PointLedgerSourceType.VOUCHER_REDEEM)
                .sourceId(userVoucher.getId())
                .status(PointLedgerStatus.REWARDED)
                .build();
        pointLedgerRepository.save(ledgerEntry);

        PointTransactionEntity voucherDeduction = PointTransactionEntity.builder()
            .user(currentUser)
            .points(lockedTemplate.getRequiredPoints().negate())
            .actionDescription(buildVoucherDeductionDescription(lockedTemplate.getName(), userVoucher.getVoucherCode()))
            .build();
        pointTransactionRepository.save(voucherDeduction);

        registerVoucherExchangeNotification(currentUser, userVoucher, lockedTemplate);

        return voucherMapper.toUserVoucherResponse(userVoucher);
    }

    /**
     * Auto-expire stale AVAILABLE vouchers before listing to keep wallet tabs consistent.
     */
    @Override
    @Transactional
    public PagedResponse<UserVoucherResponse> getCurrentUserVoucherWallet(
            UserVoucherStatus status,
            VoucherSource source,
            int page,
            int size) {
        String userId = getCurrentUserId();
        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        userVoucherRepository.expireAvailableVouchers(
                userId,
                UserVoucherStatus.AVAILABLE,
                UserVoucherStatus.EXPIRED,
                LocalDateTime.now());

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize);
            Page<UserVoucherEntity> vouchersPage = userVoucherRepository.findByUserIdWithFilters(
                userId,
                status,
                source,
                pageable);

        List<UserVoucherResponse> content = vouchersPage.getContent().stream()
                .map(voucherMapper::toUserVoucherResponse)
                .toList();

        return PagedResponse.of(
                content,
                vouchersPage.getNumber(),
                vouchersPage.getSize(),
                vouchersPage.getTotalElements(),
                vouchersPage.getTotalPages());
    }

    /**
     * Admin creates draft template and uploads partner/thumbnail images via shared file service.
     */
    @Override
    @Transactional
    public VoucherTemplateResponse createVoucherTemplate(CreateVoucherTemplateRequest request) {
        validateCreateRequest(request);

        UploadedImage partnerLogo = uploadImage(request.getPartnerLogo());
        UploadedImage thumbnail = uploadImage(request.getThumbnail());

        VoucherTemplateEntity entity = VoucherTemplateEntity.builder()
                .name(request.getName())
                .partnerName(request.getPartnerName())
                .description(request.getDescription())
                .requiredPoints(request.getRequiredPoints())
                .totalStock(request.getTotalStock())
                .remainingStock(request.getTotalStock())
                .usageConditions(request.getUsageConditions())
                .validUntil(request.getValidUntil())
                .status(VoucherTemplateStatus.DRAFT)
                .partnerLogoUrl(partnerLogo.imageUrl())
                .partnerLogoBucket(partnerLogo.bucketName())
                .partnerLogoObjectKey(partnerLogo.objectKey())
                .thumbnailUrl(thumbnail.imageUrl())
                .thumbnailBucket(thumbnail.bucketName())
                .thumbnailObjectKey(thumbnail.objectKey())
                .build();

        entity = voucherTemplateRepository.save(entity);
        return voucherMapper.toVoucherTemplateResponse(entity);
    }

    /**
     * Separate publish flow so admin can explicitly move DRAFT to ACTIVE.
     */
    @Override
    @Transactional
    public VoucherTemplateResponse updateVoucherTemplateStatus(String voucherTemplateId, VoucherTemplateStatus status) {
        VoucherTemplateEntity template = voucherTemplateRepository.findById(voucherTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));

        if (status == VoucherTemplateStatus.ACTIVE && template.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new VoucherExpiredException("Cannot activate an expired voucher template");
        }
        if (status == VoucherTemplateStatus.ACTIVE && template.getRemainingStock() <= 0) {
            throw new VoucherOutOfStockException("Cannot activate voucher template with no remaining stock");
        }

        template.setStatus(status);
        template = voucherTemplateRepository.save(template);
        return voucherMapper.toVoucherTemplateResponse(template);
    }

    @Override
    @Transactional
    public VoucherTemplateResponse updateVoucherTemplate(String voucherTemplateId, UpdateVoucherTemplateRequest request) {
        String adminId = getCurrentUserId();

        VoucherTemplateEntity template = voucherTemplateRepository.findById(voucherTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));

        List<String> changedFields = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (request.getName() != null) {
            template.setName(normalizeRequiredText(request.getName(), "Voucher name"));
            changedFields.add("name");
        }

        if (request.getPartnerName() != null) {
            template.setPartnerName(normalizeRequiredText(request.getPartnerName(), "Partner name"));
            changedFields.add("partnerName");
        }

        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
            changedFields.add("description");
        }

        if (request.getRequiredPoints() != null) {
            if (request.getRequiredPoints().compareTo(ZERO_POINTS) <= 0) {
                throw new AppException("Required points must be greater than 0", HttpStatus.BAD_REQUEST);
            }
            template.setRequiredPoints(request.getRequiredPoints());
            changedFields.add("requiredPoints");
        }

        if (request.getAdditionalStock() != null) {
            if (request.getAdditionalStock() <= 0) {
                throw new AppException("Additional stock must be greater than 0", HttpStatus.BAD_REQUEST);
            }

            int additionalStock = request.getAdditionalStock();
            template.setTotalStock(template.getTotalStock() + additionalStock);
            template.setRemainingStock(template.getRemainingStock() + additionalStock);
            changedFields.add("additionalStock");
        }

        if (request.getUsageConditions() != null) {
            template.setUsageConditions(request.getUsageConditions());
            changedFields.add("usageConditions");
        }

        if (request.getValidUntil() != null) {
            if (template.getStatus() == VoucherTemplateStatus.ACTIVE && request.getValidUntil().isBefore(now)) {
                throw new AppException("Cannot set validUntil to a past date for an active template", HttpStatus.BAD_REQUEST);
            }
            template.setValidUntil(request.getValidUntil());
            changedFields.add("validUntil");
        }

        if (request.getPartnerLogo() != null) {
            template.setPartnerLogoBucket(request.getPartnerLogo().getBucketName());
            template.setPartnerLogoObjectKey(request.getPartnerLogo().getObjectKey());
            template.setPartnerLogoUrl(request.getPartnerLogo().getImageUrl());
            changedFields.add("partnerLogo");
        }

        if (request.getThumbnail() != null) {
            template.setThumbnailBucket(request.getThumbnail().getBucketName());
            template.setThumbnailObjectKey(request.getThumbnail().getObjectKey());
            template.setThumbnailUrl(request.getThumbnail().getImageUrl());
            changedFields.add("thumbnail");
        }

        VoucherTemplateStatus requestedStatus = request.getStatus();
        if (requestedStatus != null) {
            validatePatchStatus(requestedStatus);
            changedFields.add("status");
        }

        applyStatusRulesForPatch(template, requestedStatus, now);

        template = voucherTemplateRepository.save(template);

        log.info("Admin {} updated voucher template {} fields={} status={} totalStock={} remainingStock={}",
                adminId,
                template.getId(),
                changedFields,
                template.getStatus(),
                template.getTotalStock(),
                template.getRemainingStock());

        return voucherMapper.toVoucherTemplateResponse(template);
    }

    /**
     * Programmatic voucher issue path for reward scenarios.
     * This flow does not deduct points, so it intentionally does not write point_ledger entries.
     */
    @Override
    @Transactional
    public UserVoucherEntity grantVoucherToUser(String userId, String voucherTemplateId, VoucherSource source) {
        if (userId == null || userId.isBlank()) {
            throw new AppException("User ID is required", HttpStatus.BAD_REQUEST);
        }
        if (voucherTemplateId == null || voucherTemplateId.isBlank()) {
            throw new AppException("Voucher template ID is required", HttpStatus.BAD_REQUEST);
        }
        if (source == null || source == VoucherSource.REDEEM) {
            throw new AppException("Reward source is required for programmatic voucher grants", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VoucherTemplateEntity template = voucherTemplateRepository.findByIdForUpdate(voucherTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher template not found"));

        validateVoucherTemplateForExchange(template);

        int updatedRemainingStock = template.getRemainingStock() - 1;
        template.setRemainingStock(updatedRemainingStock);
        if (updatedRemainingStock <= 0) {
            template.setStatus(VoucherTemplateStatus.DEPLETED);
        }
        voucherTemplateRepository.save(template);

        UserVoucherEntity userVoucher = UserVoucherEntity.builder()
                .user(user)
                .voucherTemplate(template)
                .voucherCode(generateUniqueVoucherCode())
                .source(source)
                .status(UserVoucherStatus.AVAILABLE)
                .expiresAt(template.getValidUntil())
                .build();

        userVoucher = userVoucherRepository.save(userVoucher);

        log.info("Granted voucher {} to user {} from template {} with source {}",
                userVoucher.getId(),
                userId,
                voucherTemplateId,
                source);

        return userVoucher;
    }

    private void validateVoucherTemplateForExchange(VoucherTemplateEntity template) {
        if (template.getStatus() != VoucherTemplateStatus.ACTIVE) {
            throw new AppException("Voucher is not available for exchange", HttpStatus.BAD_REQUEST);
        }
        if (template.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new VoucherExpiredException("Voucher has expired");
        }
        if (template.getRemainingStock() == null || template.getRemainingStock() <= 0) {
            throw new VoucherOutOfStockException("Voucher is out of stock");
        }
    }

    private void validateCreateRequest(CreateVoucherTemplateRequest request) {
        if (request.getRequiredPoints() == null || request.getRequiredPoints().compareTo(ZERO_POINTS) <= 0) {
            throw new AppException("Required points must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        if (request.getTotalStock() == null || request.getTotalStock() <= 0) {
            throw new AppException("Total stock must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        if (request.getValidUntil() == null || !request.getValidUntil().isAfter(LocalDateTime.now())) {
            throw new AppException("Valid until must be in the future", HttpStatus.BAD_REQUEST);
        }
    }

    private PointWalletEntity createDefaultWallet(UserEntity user) {
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

    private String buildVoucherDeductionDescription(String voucherName, String voucherCode) {
        String description = "Đổi voucher: " + voucherName + " (" + voucherCode + ")";
        if (description.length() <= MAX_ACTION_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_ACTION_DESCRIPTION_LENGTH);
    }

    private UploadedImage uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return UploadedImage.empty();
        }

        ImageRequestDTO uploadedImage = fileService.uploadImage(file);
        return UploadedImage.of(uploadedImage);
    }

    private record UploadedImage(String bucketName, String objectKey, String imageUrl) {
        private static UploadedImage of(ImageRequestDTO dto) {
            return new UploadedImage(dto.getBucketName(), dto.getObjectKey(), dto.getImageUrl());
        }

        private static UploadedImage empty() {
            return new UploadedImage(null, null, null);
        }
    }

    private String generateUniqueVoucherCode() {
        for (int i = 0; i < VOUCHER_CODE_MAX_RETRY; i++) {
            String code = "GV-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12)
                    .toUpperCase(Locale.ROOT);
            if (!userVoucherRepository.existsByVoucherCode(code)) {
                return code;
            }
        }
        throw new AppException("Failed to generate unique voucher code", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void validatePatchStatus(VoucherTemplateStatus status) {
        if (status == VoucherTemplateStatus.DRAFT) {
            throw new AppException("Unsupported status for update", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyStatusRulesForPatch(
            VoucherTemplateEntity template,
            VoucherTemplateStatus requestedStatus,
            LocalDateTime now) {
        if (requestedStatus != null) {
            if (requestedStatus == VoucherTemplateStatus.ACTIVE) {
                if (template.getRemainingStock() == null || template.getRemainingStock() <= 0) {
                    throw new AppException("Cannot set status ACTIVE when remaining stock is 0", HttpStatus.BAD_REQUEST);
                }
                if (template.getValidUntil() == null || template.getValidUntil().isBefore(now)) {
                    throw new AppException("Cannot set status ACTIVE when validUntil is in the past", HttpStatus.BAD_REQUEST);
                }
            }
            template.setStatus(requestedStatus);
        }

        if (template.getValidUntil() != null && template.getValidUntil().isBefore(now)) {
            template.setStatus(VoucherTemplateStatus.EXPIRED);
            return;
        }

        if (template.getRemainingStock() == null || template.getRemainingStock() <= 0) {
            template.setStatus(VoucherTemplateStatus.DEPLETED);
        }
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AppException(fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private void registerVoucherExchangeNotification(
            UserEntity user,
            UserVoucherEntity userVoucher,
            VoucherTemplateEntity voucherTemplate) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    return;
                }

                String subject = "Greenify Voucher Redeemed Successfully";
                String content = "<p>You have redeemed voucher <strong>" + voucherTemplate.getName() + "</strong>.</p>"
                        + "<p>Your voucher code: <strong>" + userVoucher.getVoucherCode() + "</strong></p>"
                        + "<p>Valid until: " + userVoucher.getExpiresAt() + "</p>";

                try {
                    emailService.sendEmail(user.getEmail(), subject, content);
                } catch (Exception ex) {
                    log.error("Failed to send voucher exchange notification for user {}", user.getId(), ex);
                }
            }
        });
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}