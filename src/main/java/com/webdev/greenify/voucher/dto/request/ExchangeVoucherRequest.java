package com.webdev.greenify.voucher.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeVoucherRequest {

    @NotBlank(message = "Voucher template ID is required")
    private String voucherTemplateId;
}