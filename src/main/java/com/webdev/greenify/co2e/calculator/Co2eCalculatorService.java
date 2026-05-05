package com.webdev.greenify.co2e.calculator;

import com.webdev.greenify.co2e.constant.Co2eMaterialCode;
import com.webdev.greenify.co2e.gemini.dto.GeminiAnalysisResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure business rules for CO2e calculation without external I/O.
 */
@Service
public class Co2eCalculatorService {

    private static final double CONFIDENCE_HIGH = 0.85;
    private static final double CONFIDENCE_MEDIUM = 0.70;
    private static final double CONFIDENCE_MIN = 0.50;
    private static final int DEFAULT_TREE_QUANTITY = 1;
    private static final int DEFAULT_QUANTITY = 1;
    private static final int CO2E_SCALE = 4;

    public Co2eCalculationResult calculate(GeminiAnalysisResponse analysisResponse,
                                            boolean alreadyCreditedReusableToday) {
        GeminiAnalysisResponse response = analysisResponse != null ? analysisResponse : new GeminiAnalysisResponse();
        Co2eMaterialCode code = parseMaterialCode(response.getMaterialCode());
        double confidence = normalizeConfidence(response.getConfidence());

        if (code == Co2eMaterialCode.UNKNOWN || confidence < CONFIDENCE_MIN) {
            return Co2eCalculationResult.skipped(code, confidence, resolveSkipReason(code, confidence));
        }

        if (code == Co2eMaterialCode.HAZARDOUS_WASTE) {
            return Co2eCalculationResult.skipped(code, confidence, "HAZARDOUS_WASTE_NO_FACTOR");
        }

        if (code == Co2eMaterialCode.TREE_PLANTING_PENDING) {
            return Co2eCalculationResult.skipped(code, confidence, "TREE_PENDING_VERIFICATION");
        }

        if (code.isReusableAction() && alreadyCreditedReusableToday) {
            return Co2eCalculationResult.skipped(code, confidence, "REUSABLE_DAILY_LIMIT_REACHED");
        }

        double confidenceMultiplier = resolveConfidenceMultiplier(confidence);
        double co2eKg;

        if (code.isReusableAction()) {
            co2eKg = code.getCo2eFactorKgPerUnit() * confidenceMultiplier;
        } else if (code.isTreePlanting()) {
            int treeCount = resolveQuantity(response, DEFAULT_TREE_QUANTITY);
            co2eKg = code.getCo2eFactorKgPerUnit() * treeCount * confidenceMultiplier;
        } else {
            double weightKg = resolveWeightKg(response, code);
            co2eKg = weightKg * code.getCo2eFactorKgPerUnit() * confidenceMultiplier;
        }

        BigDecimal co2eScaled = BigDecimal.valueOf(co2eKg).setScale(CO2E_SCALE, RoundingMode.HALF_UP);
        return Co2eCalculationResult.credited(
                code,
                co2eScaled,
                confidence,
                confidenceMultiplier,
                resolveWeightKg(response, code),
                resolveQuantity(response, null));
    }

    private double resolveConfidenceMultiplier(double confidence) {
        if (confidence >= CONFIDENCE_HIGH) {
            return 1.0;
        }
        if (confidence >= CONFIDENCE_MEDIUM) {
            return 0.8;
        }
        return 0.5;
    }

    private double resolveWeightKg(GeminiAnalysisResponse response, Co2eMaterialCode code) {
        if (response.getEstimatedWeightKg() != null && response.getEstimatedWeightKg() > 0) {
            return response.getEstimatedWeightKg();
        }
        if (code.getDefaultWeightKgPerItem() != null && response.getQuantity() != null) {
            return code.getDefaultWeightKgPerItem() * response.getQuantity();
        }
        if (code.getDefaultWeightKgPerItem() != null) {
            return code.getDefaultWeightKgPerItem();
        }
        return 0.0;
    }

    private int resolveQuantity(GeminiAnalysisResponse response, Integer defaultQty) {
        if (response.getQuantity() != null && response.getQuantity() > 0) {
            return response.getQuantity();
        }
        if (defaultQty != null) {
            return defaultQty;
        }
        return DEFAULT_QUANTITY;
    }

    private String resolveSkipReason(Co2eMaterialCode code, double confidence) {
        if (code == Co2eMaterialCode.UNKNOWN) {
            return "AI_RETURNED_UNKNOWN";
        }
        if (confidence < CONFIDENCE_MIN) {
            return "LOW_CONFIDENCE_" + String.format("%.2f", confidence);
        }
        return "UNKNOWN_REASON";
    }

    private Co2eMaterialCode parseMaterialCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Co2eMaterialCode.UNKNOWN;
        }
        try {
            return Co2eMaterialCode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Co2eMaterialCode.UNKNOWN;
        }
    }

    private double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }
}
