package com.webdev.greenify.garden.controller;

import com.webdev.greenify.garden.dto.request.CreateSeedRequest;
import com.webdev.greenify.garden.dto.request.SelectSeedRequest;
import com.webdev.greenify.garden.dto.request.UpdateSeedRequest;
import com.webdev.greenify.garden.dto.response.GardenArchiveResponse;
import com.webdev.greenify.garden.dto.response.PlantDailyLogResponse;
import com.webdev.greenify.garden.dto.response.PlantProgressResponse;
import com.webdev.greenify.garden.dto.response.SeedResponse;
import com.webdev.greenify.garden.service.GardenService;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.voucher.dto.response.VoucherTemplateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GardenController {

    private final GardenService gardenService;

    @GetMapping("/garden/seeds")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<PagedResponse<SeedResponse>> getAvailableSeeds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gardenService.getAvailableSeeds(page, size));
    }

    @GetMapping("/garden/seeds/{seedId}/reward-voucher")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<VoucherTemplateResponse> getRewardVoucherTemplateBySeedId(@PathVariable String seedId) {
        return ResponseEntity.ok(gardenService.getRewardVoucherTemplateBySeedId(seedId));
    }

    @PostMapping("/garden/plant")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<PlantProgressResponse> selectSeed(@Valid @RequestBody SelectSeedRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gardenService.selectSeed(request));
    }

    @GetMapping("/garden/plant/current")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<PlantProgressResponse> getCurrentPlantProgress() {
        return ResponseEntity.ok(gardenService.getCurrentPlantProgress());
    }

    @GetMapping("/garden/plant/daily-logs")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<List<PlantDailyLogResponse>> getCurrentUserDailyLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(gardenService.getCurrentUserDailyLogs(fromDate, toDate));
    }

    @GetMapping("/garden/archives")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<PagedResponse<GardenArchiveResponse>> getGardenArchives(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gardenService.getGardenArchives(page, size));
    }

    @PostMapping("/admin/garden/seeds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeedResponse> createSeed(@Valid @RequestBody CreateSeedRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gardenService.createSeed(request));
    }

    @PatchMapping("/admin/garden/seeds/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeedResponse> updateSeed(
            @PathVariable String id,
            @Valid @RequestBody UpdateSeedRequest request) {
        return ResponseEntity.ok(gardenService.updateSeed(id, request));
    }
}
