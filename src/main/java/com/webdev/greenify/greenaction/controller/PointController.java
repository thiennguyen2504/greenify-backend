package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.dto.response.PointHistoryResponse;
import com.webdev.greenify.greenaction.dto.response.TotalPointsResponse;
import com.webdev.greenify.greenaction.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/green-action/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * Get total points for current logged-in user.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<TotalPointsResponse> getTotalPoints() {
        return ResponseEntity.ok(pointService.getTotalPointsForCurrentUser());
    }

    /**
     * Get point history for current logged-in user with pagination.
     */
    @GetMapping("/me/history")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<PagedResponse<PointHistoryResponse>> getPointHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(pointService.getPointHistoryForCurrentUser(page, size));
    }
}
