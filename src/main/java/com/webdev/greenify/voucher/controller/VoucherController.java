package com.webdev.greenify.voucher.controller;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.voucher.dto.request.CreateVoucherTemplateRequest;
import com.webdev.greenify.voucher.dto.request.ExchangeVoucherRequest;
import com.webdev.greenify.voucher.dto.response.UserVoucherResponse;
import com.webdev.greenify.voucher.dto.response.VoucherMarketplaceResponse;
import com.webdev.greenify.voucher.dto.response.VoucherTemplateResponse;
import com.webdev.greenify.voucher.enumeration.UserVoucherStatus;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;
import com.webdev.greenify.voucher.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/vouchers")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<VoucherMarketplaceResponse> getAvailableVouchers(
            @RequestParam(required = false) BigDecimal minRequiredPoints,
            @RequestParam(required = false) BigDecimal maxRequiredPoints,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(voucherService.getAvailableVouchers(
                minRequiredPoints,
                maxRequiredPoints,
                page,
                size));
    }

    @PostMapping("/vouchers/{id}/exchange")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<UserVoucherResponse> exchangeVoucher(@PathVariable String id) {
        ExchangeVoucherRequest request = ExchangeVoucherRequest.builder()
                .voucherTemplateId(id)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.exchangeVoucher(request));
    }

    @GetMapping("/wallet/vouchers")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<PagedResponse<UserVoucherResponse>> getUserVoucherWallet(
            @RequestParam(required = false) UserVoucherStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(voucherService.getCurrentUserVoucherWallet(status, page, size));
    }

    @PostMapping("/admin/vouchers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherTemplateResponse> createVoucherTemplate(
            @Valid @RequestBody CreateVoucherTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucherTemplate(request));
    }

    @PatchMapping("/admin/vouchers/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherTemplateResponse> updateVoucherTemplateStatus(
            @PathVariable String id,
            @RequestParam VoucherTemplateStatus status) {
        return ResponseEntity.ok(voucherService.updateVoucherTemplateStatus(id, status));
    }
}