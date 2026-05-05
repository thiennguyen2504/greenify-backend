package com.webdev.greenify.co2e.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.co2e.enumeration.Co2eTransactionStatus;
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
public class PostCo2eResponse {

    private String postId;
    private Co2eTransactionStatus status;
    private BigDecimal co2eKg;
    private String materialCode;
    private String materialLabel;
    private String skipReason;
}
