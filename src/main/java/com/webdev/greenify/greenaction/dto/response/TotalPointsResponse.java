package com.webdev.greenify.greenaction.dto.response;

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
public class TotalPointsResponse {

    private String userId;
    
    private BigDecimal accumulatedPoints;
    
    private BigDecimal availablePoints;
    
    private long transactionCount;
}
