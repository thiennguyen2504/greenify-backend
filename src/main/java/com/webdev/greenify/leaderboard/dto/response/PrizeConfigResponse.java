package com.webdev.greenify.leaderboard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrizeConfigResponse {

    private String id;
    private LocalDate weekStartDate;
    private LocalDateTime lockAt;
    private PrizeConfigStatus status;
    private String nationalVoucherTemplateId;
    private String provincialVoucherTemplateId;
    private Integer nationalReservedCount;
    private Integer provincialReservedCount;
    private LocalDateTime distributedAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
