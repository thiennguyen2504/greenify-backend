package com.webdev.greenify.voucher.service;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.voucher.dto.request.CreateVoucherTemplateRequest;
import com.webdev.greenify.voucher.dto.request.ExchangeVoucherRequest;
import com.webdev.greenify.voucher.dto.response.UserVoucherResponse;
import com.webdev.greenify.voucher.dto.response.VoucherMarketplaceResponse;
import com.webdev.greenify.voucher.dto.response.VoucherTemplateResponse;
import com.webdev.greenify.voucher.enumeration.UserVoucherStatus;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;

import java.math.BigDecimal;

public interface VoucherService {

    /**
     * Get ACTIVE vouchers in marketplace with stock/time constraints and optional point-range filter.
     * Response includes current user's available points for affordability highlighting.
     */
    VoucherMarketplaceResponse getAvailableVouchers(
            BigDecimal minRequiredPoints,
            BigDecimal maxRequiredPoints,
            int page,
            int size);

    /**
     * Exchange points for a voucher in one atomic transaction.
     */
    UserVoucherResponse exchangeVoucher(ExchangeVoucherRequest request);

    /**
     * Get current user's voucher wallet with optional status filtering and pagination.
     * Expired AVAILABLE vouchers are auto-marked as EXPIRED before query.
     */
    PagedResponse<UserVoucherResponse> getCurrentUserVoucherWallet(UserVoucherStatus status, int page, int size);

    /**
     * Create a voucher template as DRAFT and initialize remaining stock = total stock.
     */
    VoucherTemplateResponse createVoucherTemplate(CreateVoucherTemplateRequest request);

    /**
     * Update voucher template status for explicit publish/unpublish flow.
     */
    VoucherTemplateResponse updateVoucherTemplateStatus(String voucherTemplateId, VoucherTemplateStatus status);
}