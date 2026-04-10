package com.webdev.greenify.voucher.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;
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
public class VoucherTemplateResponse {

    private String id;
    private String name;
    private String partnerName;
    private String description;
    private BigDecimal requiredPoints;
    private Integer totalStock;
    private Integer remainingStock;
    private String usageConditions;
    private LocalDateTime validUntil;
    private String partnerLogoUrl;
    private String partnerLogoBucket;
    private String partnerLogoObjectKey;
    private String thumbnailUrl;
    private String thumbnailBucket;
    private String thumbnailObjectKey;
    private VoucherTemplateStatus status;
}