package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.request.CreateAppealRequest;
import com.webdev.greenify.greenaction.dto.request.ReviewAppealRequest;
import com.webdev.greenify.greenaction.dto.request.UpdateAppealRequest;
import com.webdev.greenify.greenaction.dto.response.AppealResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.AppealStatus;
import com.webdev.greenify.greenaction.service.AppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/green-action/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AppealResponse> createAppeal(@Valid @RequestBody CreateAppealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appealService.createAppeal(request));
    }

    @GetMapping("/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<AppealResponse>> getAppealsForReview(
            @RequestParam(required = false) AppealStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(appealService.getAppealsForReview(status, page, size));
    }

    @GetMapping("/{appealId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AppealResponse> getAppealDetail(@PathVariable String appealId) {
        return ResponseEntity.ok(appealService.getAppealDetail(appealId));
    }

    @PutMapping("/{appealId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AppealResponse> updateAppeal(
            @PathVariable String appealId,
            @Valid @RequestBody UpdateAppealRequest request) {
        return ResponseEntity.ok(appealService.updateAppeal(appealId, request));
    }

    @PostMapping("/{appealId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppealResponse> reviewAppeal(
            @PathVariable String appealId,
            @Valid @RequestBody ReviewAppealRequest request) {
        return ResponseEntity.ok(appealService.reviewAppeal(appealId, request));
    }
}
