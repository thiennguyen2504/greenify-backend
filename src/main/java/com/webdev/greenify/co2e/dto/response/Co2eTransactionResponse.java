package com.webdev.greenify.co2e.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.co2e.enumeration.Co2eType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Co2eTransactionResponse {

    private String postId;
    private String materialCode;
    private String materialLabel;
    private Co2eType co2eType;
    private BigDecimal co2eKg;
    private BigDecimal confidenceScore;
    private LocalDateTime creditedAt;
}
