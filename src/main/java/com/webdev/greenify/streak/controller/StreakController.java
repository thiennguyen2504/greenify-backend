package com.webdev.greenify.streak.controller;

import com.webdev.greenify.streak.dto.request.RestoreStreakRequest;
import com.webdev.greenify.streak.dto.response.StreakResponse;
import com.webdev.greenify.streak.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<StreakResponse> getCurrentStreak() {
        return ResponseEntity.ok(streakService.getCurrentStreak());
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<StreakResponse> restoreStreak(@RequestBody(required = false) RestoreStreakRequest request) {
        return ResponseEntity.ok(streakService.restoreStreak());
    }
}
