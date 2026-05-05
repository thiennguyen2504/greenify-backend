package com.webdev.greenify.co2e.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GreenImpactWalletResponse {

    private BigDecimal totalAvoidedKg;
    private BigDecimal totalAbsorbedKg;
    private BigDecimal totalCo2eKg;
}
