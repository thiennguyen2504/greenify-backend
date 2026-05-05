package com.webdev.greenify.co2e.service.impl;

import com.webdev.greenify.co2e.calculator.Co2eCalculationResult;
import com.webdev.greenify.co2e.calculator.Co2eCalculatorService;
import com.webdev.greenify.co2e.constant.Co2eMaterialCode;
import com.webdev.greenify.co2e.dto.response.Co2eTransactionResponse;
import com.webdev.greenify.co2e.dto.response.GreenImpactWalletResponse;
import com.webdev.greenify.co2e.dto.response.PostCo2eResponse;
import com.webdev.greenify.co2e.entity.Co2eTransactionEntity;
import com.webdev.greenify.co2e.entity.GreenImpactWalletEntity;
import com.webdev.greenify.co2e.enumeration.Co2eTransactionStatus;
import com.webdev.greenify.co2e.enumeration.Co2eType;
import com.webdev.greenify.co2e.event.Co2eAnalysisEvent;
import com.webdev.greenify.co2e.gemini.GeminiAnalysisService;
import com.webdev.greenify.co2e.gemini.dto.GeminiAnalysisResponse;
import com.webdev.greenify.co2e.mapper.Co2eMapper;
import com.webdev.greenify.co2e.repository.Co2eTransactionRepository;
import com.webdev.greenify.co2e.repository.GreenImpactWalletRepository;
import com.webdev.greenify.co2e.service.Co2eService;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Coordinates CO2e analysis, persistence, and wallet aggregation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Co2eServiceImpl implements Co2eService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final BigDecimal ZERO_KG = new BigDecimal("0.0000");

    private final Co2eTransactionRepository co2eTransactionRepository;
    private final GreenImpactWalletRepository greenImpactWalletRepository;
    private final Co2eCalculatorService co2eCalculatorService;
    private final GeminiAnalysisService geminiAnalysisService;
    private final Co2eMapper co2eMapper;

    @Override
    @Transactional
    public void processPostCo2e(Co2eAnalysisEvent event) {
        if (co2eTransactionRepository.existsByPostId(event.getPostId())) {
            log.info("CO2e already processed for post {}, skipping", event.getPostId());
            return;
        }

        GeminiAnalysisResponse aiResult = geminiAnalysisService.analyzePost(
                event.getImageUrl(),
                event.getCaption(),
                event.getActionTypeName());

        if (aiResult == null || !aiResult.isAvailable()) {
            Co2eTransactionEntity transaction = buildGeminiUnavailableTransaction(event, aiResult);
            co2eTransactionRepository.save(transaction);
            return;
        }

        boolean alreadyCreditedReusable = isReusableCreditedToday(event.getUserId(), event.getActionDate());
        Co2eCalculationResult calcResult = co2eCalculatorService.calculate(aiResult, alreadyCreditedReusable);

        Co2eTransactionEntity transaction = buildTransaction(event, calcResult);
        co2eTransactionRepository.save(transaction);

        if (calcResult.isCredited()) {
            updateWallet(event.getUserId(), calcResult);
        }

        log.info("CO2e processed for post {}: status={}, co2eKg={}, materialCode={}",
                event.getPostId(),
                calcResult.getStatus(),
                calcResult.getCo2eKg(),
                calcResult.getMaterialCode());
    }

    @Override
    @Transactional(readOnly = true)
    public GreenImpactWalletResponse getWalletForCurrentUser() {
        String userId = getCurrentUserId();
        GreenImpactWalletEntity wallet = greenImpactWalletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return GreenImpactWalletResponse.builder()
                    .totalAvoidedKg(ZERO_KG)
                    .totalAbsorbedKg(ZERO_KG)
                    .totalCo2eKg(ZERO_KG)
                    .build();
        }
        return co2eMapper.toWalletResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Co2eTransactionResponse> getCo2eHistoryForCurrentUser(int page, int size) {
        String userId = getCurrentUserId();

        int effectivePage = Math.max(page, 0);
        int effectiveSize = clampPageSize(size);

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize);
        Page<Co2eTransactionEntity> transactionsPage = co2eTransactionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, Co2eTransactionStatus.CREDITED, pageable);

        List<Co2eTransactionResponse> content = transactionsPage.getContent().stream()
                .map(co2eMapper::toTransactionResponse)
                .toList();

        return PagedResponse.of(
                content,
                transactionsPage.getNumber(),
                transactionsPage.getSize(),
                transactionsPage.getTotalElements(),
                transactionsPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PostCo2eResponse getCo2eForPost(String postId) {
        return co2eTransactionRepository.findByPostId(postId)
                .map(this::toPostCo2eResponse)
                .orElseGet(() -> PostCo2eResponse.builder()
                        .postId(postId)
                        .status(Co2eTransactionStatus.PENDING)
                        .build());
    }

    private PostCo2eResponse toPostCo2eResponse(Co2eTransactionEntity transaction) {
        return PostCo2eResponse.builder()
                .postId(transaction.getPostId())
                .status(transaction.getStatus())
                .co2eKg(transaction.getCo2eKg())
                .materialCode(transaction.getMaterialCode())
                .materialLabel(transaction.getMaterialLabel())
                .skipReason(transaction.getSkipReason())
                .build();
    }

    private boolean isReusableCreditedToday(String userId, LocalDate actionDate) {
        if (actionDate == null) {
            return false;
        }
        return co2eTransactionRepository.existsReusableCreditedByUserOnDate(userId, actionDate);
    }

    private Co2eTransactionEntity buildGeminiUnavailableTransaction(
            Co2eAnalysisEvent event,
            GeminiAnalysisResponse aiResult) {
        double confidence = aiResult != null && aiResult.getConfidence() != null
            ? aiResult.getConfidence()
            : 0.0;

        return Co2eTransactionEntity.builder()
                .postId(event.getPostId())
                .userId(event.getUserId())
                .co2eType(Co2eType.AVOIDED)
                .co2eKg(ZERO_KG)
                .status(Co2eTransactionStatus.SKIPPED)
                .materialCode(Co2eMaterialCode.UNKNOWN.name())
                .materialLabel(Co2eMaterialCode.UNKNOWN.getLabel())
                .confidenceScore(BigDecimal.valueOf(confidence).setScale(4, java.math.RoundingMode.HALF_UP))
                .confidenceMultiplier(BigDecimal.ZERO)
                .estimatedWeightKg(BigDecimal.ZERO)
                .quantity(null)
                .skipReason("GEMINI_UNAVAILABLE")
                .build();
    }

    private Co2eTransactionEntity buildTransaction(
            Co2eAnalysisEvent event,
            Co2eCalculationResult result) {
        Co2eMaterialCode materialCode = result.getMaterialCode();
        return Co2eTransactionEntity.builder()
                .postId(event.getPostId())
                .userId(event.getUserId())
                .co2eType(result.getCo2eType())
                .co2eKg(result.getCo2eKg())
                .status(result.getStatus())
                .materialCode(materialCode != null ? materialCode.name() : null)
                .materialLabel(materialCode != null ? materialCode.getLabel() : null)
                .confidenceScore(result.getConfidenceScore())
                .confidenceMultiplier(result.getConfidenceMultiplier())
                .estimatedWeightKg(result.getEstimatedWeightKg())
                .quantity(result.getQuantity())
                .skipReason(result.getSkipReason())
                .build();
    }

    private void updateWallet(String userId, Co2eCalculationResult result) {
        GreenImpactWalletEntity wallet = getOrCreateWalletForUpdate(userId);
        BigDecimal amount = result.getCo2eKg() != null ? result.getCo2eKg() : ZERO_KG;

        if (result.getCo2eType() == Co2eType.ABSORBED) {
            BigDecimal current = wallet.getTotalAbsorbedKg() != null ? wallet.getTotalAbsorbedKg() : ZERO_KG;
            wallet.setTotalAbsorbedKg(current.add(amount));
        } else {
            BigDecimal current = wallet.getTotalAvoidedKg() != null ? wallet.getTotalAvoidedKg() : ZERO_KG;
            wallet.setTotalAvoidedKg(current.add(amount));
        }

        greenImpactWalletRepository.save(wallet);
    }

    private GreenImpactWalletEntity getOrCreateWalletForUpdate(String userId) {
        return greenImpactWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> bootstrapWallet(userId));
    }

    private GreenImpactWalletEntity bootstrapWallet(String userId) {
        GreenImpactWalletEntity wallet = GreenImpactWalletEntity.builder()
                .userId(userId)
                .totalAvoidedKg(ZERO_KG)
                .totalAbsorbedKg(ZERO_KG)
                .build();

        try {
            return greenImpactWalletRepository.save(wallet);
        } catch (DataIntegrityViolationException ex) {
            return greenImpactWalletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> ex);
        }
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
