package com.webdev.greenify.co2e.calculator;

import com.webdev.greenify.co2e.constant.Co2eMaterialCode;
import com.webdev.greenify.co2e.enumeration.Co2eTransactionStatus;
import com.webdev.greenify.co2e.enumeration.Co2eType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Co2eCalculationResult {

    private static final int CONFIDENCE_SCALE = 4;
    private static final BigDecimal ZERO_CO2E = new BigDecimal("0.0000");

    private Co2eMaterialCode materialCode;
    private Co2eTransactionStatus status;
    private BigDecimal co2eKg;
    private BigDecimal confidenceScore;
    private BigDecimal confidenceMultiplier;
    private BigDecimal estimatedWeightKg;
    private Integer quantity;
    private String skipReason;

    public static Co2eCalculationResult skipped(Co2eMaterialCode code, double confidence, String reason) {
        return Co2eCalculationResult.builder()
                .materialCode(code)
                .status(Co2eTransactionStatus.SKIPPED)
                .co2eKg(ZERO_CO2E)
                .confidenceScore(scaleConfidence(confidence))
            .confidenceMultiplier(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .estimatedWeightKg(BigDecimal.ZERO)
                .quantity(null)
                .skipReason(reason)
                .build();
    }

    public static Co2eCalculationResult credited(
            Co2eMaterialCode code,
            BigDecimal co2eKg,
            double confidence,
            double confidenceMultiplier,
            Double estimatedWeightKg,
            Integer quantity) {
        return Co2eCalculationResult.builder()
                .materialCode(code)
                .status(Co2eTransactionStatus.CREDITED)
                .co2eKg(co2eKg)
                .confidenceScore(scaleConfidence(confidence))
                .confidenceMultiplier(BigDecimal.valueOf(confidenceMultiplier)
                    .setScale(2, RoundingMode.HALF_UP))
                .estimatedWeightKg(estimatedWeightKg != null
                        ? BigDecimal.valueOf(estimatedWeightKg).setScale(4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .quantity(quantity)
                .build();
    }

    public boolean isCredited() {
        return status == Co2eTransactionStatus.CREDITED;
    }

    public Co2eType getCo2eType() {
        return materialCode != null ? materialCode.getCo2eType() : null;
    }

    private static BigDecimal scaleConfidence(double confidence) {
        return BigDecimal.valueOf(confidence).setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }
}
