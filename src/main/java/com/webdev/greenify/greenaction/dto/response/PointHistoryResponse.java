package com.webdev.greenify.greenaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class PointHistoryResponse {

    private String id;
    private BigDecimal points;
    private String actionDescription;
    private String sourcePostId;
    private String sourceReviewId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String expiredTransactionId;
}
