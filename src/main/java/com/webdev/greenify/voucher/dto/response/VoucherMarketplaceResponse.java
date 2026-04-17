package com.webdev.greenify.voucher.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoucherMarketplaceResponse {

    private BigDecimal availablePoints;
    private List<VoucherTemplateResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static VoucherMarketplaceResponse of(
            BigDecimal availablePoints,
            List<VoucherTemplateResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
        return VoucherMarketplaceResponse.builder()
                .availablePoints(availablePoints)
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}