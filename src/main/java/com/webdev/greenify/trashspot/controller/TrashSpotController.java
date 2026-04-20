package com.webdev.greenify.trashspot.controller;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.trashspot.dto.request.CreateResolveRequestRequest;
import com.webdev.greenify.trashspot.dto.request.CreateTrashSpotRequest;
import com.webdev.greenify.trashspot.dto.request.CreateTrashSpotReportRequest;
import com.webdev.greenify.trashspot.dto.request.ReviewResolveRequest;
import com.webdev.greenify.trashspot.dto.request.SubmitVerificationRequest;
import com.webdev.greenify.trashspot.dto.response.ResolveRequestResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotDetailResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotReportResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotSummaryResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotVerificationResponse;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import com.webdev.greenify.trashspot.enumeration.SeverityTier;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;
import com.webdev.greenify.trashspot.service.TrashSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrashSpotController {

    private final TrashSpotService trashSpotService;

    @PostMapping("/trash-spots")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
    public ResponseEntity<TrashSpotDetailResponse> createOrMergeTrashSpot(
            @Valid @RequestBody CreateTrashSpotRequest request) {
        TrashSpotService.CreateOrMergeResult result = trashSpotService.createOrMerge(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping("/trash-spots")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<List<TrashSpotSummaryResponse>> getTrashSpots(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) TrashSpotStatus status,
            @RequestParam(name = "severityTier", required = false) SeverityTier severityTier,
            @RequestParam(name = "severity", required = false) SeverityTier legacySeverity,
            @RequestParam(name = "wasteTypeId", required = false) String wasteTypeId,
            @RequestParam(name = "wasteTypeID", required = false) String legacyWasteTypeId) {
        String normalizedProvince = normalizeOptionalTextParam(province);
        SeverityTier resolvedSeverity = resolveSeverity(severityTier, legacySeverity);
        String resolvedWasteTypeId = resolveOptionalTextParam(wasteTypeId, legacyWasteTypeId);

        return ResponseEntity.ok(trashSpotService.getTrashSpots(
                normalizedProvince,
                status,
                resolvedSeverity,
                resolvedWasteTypeId));
    }

    @GetMapping("/trash-spots/{id}")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<TrashSpotDetailResponse> getTrashSpotDetail(@PathVariable String id) {
        return ResponseEntity.ok(trashSpotService.getTrashSpotDetail(id));
    }

    @PostMapping("/trash-spots/{id}/verifications")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
    public ResponseEntity<TrashSpotVerificationResponse> submitVerification(
            @PathVariable String id,
            @RequestBody(required = false) SubmitVerificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trashSpotService.submitVerification(id, request));
    }

        @PostMapping("/trash-spots/{id}/reports")
        @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
        public ResponseEntity<TrashSpotReportResponse> reportTrashSpot(
            @PathVariable String id,
            @Valid @RequestBody CreateTrashSpotReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(trashSpotService.reportTrashSpot(id, request));
        }

    @GetMapping("/ngo/trash-spots")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<TrashSpotSummaryResponse>> getNgoTrashSpots(
            @RequestParam(required = false) String province,
            @RequestParam(name = "severityTier", required = false) SeverityTier severityTier,
            @RequestParam(name = "severity", required = false) SeverityTier legacySeverity,
            @RequestParam(name = "wasteTypeId", required = false) String wasteTypeId,
            @RequestParam(name = "wasteTypeID", required = false) String legacyWasteTypeId) {
        String normalizedProvince = normalizeOptionalTextParam(province);
        SeverityTier resolvedSeverity = resolveSeverity(severityTier, legacySeverity);
        String resolvedWasteTypeId = resolveOptionalTextParam(wasteTypeId, legacyWasteTypeId);

        return ResponseEntity.ok(trashSpotService.getNgoTrashSpots(
                normalizedProvince,
                resolvedSeverity,
                resolvedWasteTypeId));
    }

    @PatchMapping("/ngo/trash-spots/{id}/claim")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<TrashSpotDetailResponse> claimSpot(@PathVariable String id) {
        return ResponseEntity.ok(trashSpotService.claimSpot(id));
    }

    @PostMapping("/ngo/trash-spots/{id}/resolve-requests")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<ResolveRequestResponse> createResolveRequest(
            @PathVariable String id,
            @Valid @RequestBody CreateResolveRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trashSpotService.createResolveRequest(id, request));
    }

    @GetMapping("/admin/trash-spots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TrashSpotSummaryResponse>> getAdminTrashSpots(
            @RequestParam(required = false) TrashSpotStatus status,
            @RequestParam(required = false) String province,
            @RequestParam(name = "severityTier", required = false) SeverityTier severityTier,
            @RequestParam(name = "severity", required = false) SeverityTier legacySeverity,
            @RequestParam(name = "wasteTypeId", required = false) String wasteTypeId,
            @RequestParam(name = "wasteTypeID", required = false) String legacyWasteTypeId) {
        String normalizedProvince = normalizeOptionalTextParam(province);
        SeverityTier resolvedSeverity = resolveSeverity(severityTier, legacySeverity);
        String resolvedWasteTypeId = resolveOptionalTextParam(wasteTypeId, legacyWasteTypeId);

        return ResponseEntity.ok(trashSpotService.getAdminTrashSpots(
                status,
                normalizedProvince,
                resolvedSeverity,
                resolvedWasteTypeId));
    }

    @DeleteMapping("/admin/trash-spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTrashSpot(@PathVariable String id) {
        trashSpotService.deleteTrashSpot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/trash-spots/resolve-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ResolveRequestResponse>> getResolveRequests(
            @RequestParam(required = false) ResolveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(trashSpotService.getResolveRequests(status, page, size));
    }

    @GetMapping("/admin/trash-spots/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<TrashSpotReportResponse>> getTrashSpotReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(trashSpotService.getTrashSpotReports(page, size));
    }

    @PostMapping("/admin/trash-spots/resolve-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResolveRequestResponse> approveResolveRequest(@PathVariable String id) {
        return ResponseEntity.ok(trashSpotService.approveResolveRequest(id));
    }

    @PostMapping("/admin/trash-spots/resolve-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResolveRequestResponse> rejectResolveRequest(
            @PathVariable String id,
            @Valid @RequestBody ReviewResolveRequest request) {
        return ResponseEntity.ok(trashSpotService.rejectResolveRequest(id, request));
    }

    @PatchMapping("/admin/trash-spots/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrashSpotDetailResponse> reopenResolvedSpot(@PathVariable String id) {
        return ResponseEntity.ok(trashSpotService.reopenResolvedSpot(id));
    }

    private SeverityTier resolveSeverity(SeverityTier severityTier, SeverityTier legacySeverity) {
        return severityTier != null ? severityTier : legacySeverity;
    }

    private String resolveOptionalTextParam(String preferredValue, String fallbackValue) {
        String normalizedPreferred = normalizeOptionalTextParam(preferredValue);
        return normalizedPreferred != null ? normalizedPreferred : normalizeOptionalTextParam(fallbackValue);
    }

    private String normalizeOptionalTextParam(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        String lowered = normalized.toLowerCase();
        if ("undefined".equals(lowered) || "null".equals(lowered)) {
            return null;
        }

        return normalized;
    }
}
