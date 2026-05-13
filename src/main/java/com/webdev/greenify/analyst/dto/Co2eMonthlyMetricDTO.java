package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Co2eMonthlyMetricDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String month;
    private BigDecimal totalCo2eKg;
    private BigDecimal totalAvoidedKg;
    private BigDecimal totalAbsorbedKg;
}
