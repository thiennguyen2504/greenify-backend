package com.webdev.greenify.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CtvEligibilityResponseDTO {
    private BigDecimal accumulatedPoints;
    private boolean eligibleForCtv;
}