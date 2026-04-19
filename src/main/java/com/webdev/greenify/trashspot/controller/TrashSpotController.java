package com.webdev.greenify.trashspot.controller;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.trashspot.dto.request.CreateResolveRequestRequest;
import com.webdev.greenify.trashspot.dto.request.CreateTrashSpotRequest;
import com.webdev.greenify.trashspot.dto.request.ReviewResolveRequest;
import com.webdev.greenify.trashspot.dto.request.SubmitVerificationRequest;
import com.webdev.greenify.trashspot.dto.response.ResolveRequestResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotDetailResponse;
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
            @RequestParam(required = false) SeverityTier severity,
            @RequestParam(name = "wasteTypeID", required = false) String wasteTypeId) {
        return ResponseEntity.ok(trashSpotService.getTrashSpots(province, status, severity, wasteTypeId));
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

    @GetMapping("/ngo/trash-spots")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<TrashSpotSummaryResponse>> getNgoTrashSpots(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) SeverityTier severity,
            @RequestParam(name = "wasteTypeID", required = false) String wasteTypeId) {
        return ResponseEntity.ok(trashSpotService.getNgoTrashSpots(province, severity, wasteTypeId));
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
            @RequestParam(required = false) SeverityTier severity,
            @RequestParam(name = "wasteTypeID", required = false) String wasteTypeId) {
        return ResponseEntity.ok(trashSpotService.getAdminTrashSpots(status, province, severity, wasteTypeId));
    }

    @GetMapping("/admin/trash-spots/resolve-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ResolveRequestResponse>> getResolveRequests(
            @RequestParam(required = false) ResolveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(trashSpotService.getResolveRequests(status, page, size));
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
}
