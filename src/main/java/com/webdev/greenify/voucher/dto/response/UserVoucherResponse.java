package com.webdev.greenify.voucher.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.voucher.enumeration.UserVoucherStatus;
import com.webdev.greenify.voucher.enumeration.VoucherSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucherResponse {

    private String id;
    private String voucherCode;
    private String voucherTemplateId;
    private String voucherName;
    private String partnerName;
    private String partnerLogoUrl;
    private String description;
    private String usageConditions;
    private String thumbnailUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private UserVoucherStatus status;
    private VoucherSource source;
}