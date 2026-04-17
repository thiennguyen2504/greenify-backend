package com.webdev.greenify.leaderboard.controller;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.leaderboard.dto.request.CreatePrizeConfigRequest;
import com.webdev.greenify.leaderboard.dto.response.LeaderboardPrizeResponse;
import com.webdev.greenify.leaderboard.dto.response.LeaderboardResponse;
import com.webdev.greenify.leaderboard.dto.response.PrizeConfigResponse;
import com.webdev.greenify.leaderboard.enumeration.LeaderboardScope;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;
import com.webdev.greenify.leaderboard.service.LeaderboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @PostMapping(value = "/admin/leaderboard/prizes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrizeConfigResponse> createPrizeConfig(
            @Valid @ModelAttribute CreatePrizeConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaderboardService.createPrizeConfig(request));
    }

    @GetMapping("/admin/leaderboard/prizes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<PrizeConfigResponse>> getPrizeConfigs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @RequestParam(required = false) PrizeConfigStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaderboardService.getPrizeConfigs(weekStartDate, status, page, size));
    }

    @GetMapping("/admin/leaderboard/prizes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrizeConfigResponse> getPrizeConfigById(@PathVariable String id) {
        return ResponseEntity.ok(leaderboardService.getPrizeConfigById(id));
    }

    @DeleteMapping("/admin/leaderboard/prizes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelPrizeConfig(@PathVariable String id) {
        leaderboardService.cancelPrizeConfig(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/leaderboard/prizes/{id}/distribute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> distributePrizeConfig(@PathVariable String id) {
        leaderboardService.finalizeWeek(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/leaderboard/weekly")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<LeaderboardResponse> getWeeklyLeaderboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @RequestParam(defaultValue = "NATIONAL") LeaderboardScope scope,
            @RequestParam(required = false) String province) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(weekStartDate, scope, province));
    }

    @GetMapping("/leaderboard/weekly/prizes")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<LeaderboardPrizeResponse> getWeeklyPrize(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return ResponseEntity.ok(leaderboardService.getLeaderboardPrize(weekStartDate));
    }
}
