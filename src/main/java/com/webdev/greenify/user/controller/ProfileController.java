package com.webdev.greenify.user.controller;

import com.webdev.greenify.user.dto.UserProfileCreateRequestDTO;
import com.webdev.greenify.user.dto.UserProfileResponseDTO;
import com.webdev.greenify.user.dto.UserProfileUpdateRequestDTO;
import com.webdev.greenify.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfileResponseDTO> getCurrentUserProfile() {
        return ResponseEntity.ok(profileService.getCurrentUserProfile());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfileResponseDTO> createProfile(
            @Valid @RequestBody UserProfileCreateRequestDTO request) {
        return ResponseEntity.ok(profileService.createCurrentUserProfile(request));
    }

    @PutMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfileResponseDTO> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequestDTO request) {
        return ResponseEntity.ok(profileService.updateCurrentUserProfile(request));
    }
}
