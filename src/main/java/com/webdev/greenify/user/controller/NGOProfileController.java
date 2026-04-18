package com.webdev.greenify.user.controller;

import com.webdev.greenify.user.dto.NGOProfileFilterRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileRejectRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileResponseDTO;
import com.webdev.greenify.user.dto.PagedResponse;
import com.webdev.greenify.user.service.NGOProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ngo-profiles")
@RequiredArgsConstructor
public class NGOProfileController {

    private final NGOProfileService ngoProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('NGO', 'USER')")
    public ResponseEntity<NGOProfileResponseDTO> getCurrentNGOProfile() {
        return ResponseEntity.ok(ngoProfileService.getCurrentNGOProfile());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<NGOProfileResponseDTO>> searchNGOProfiles(
            NGOProfileFilterRequestDTO filter) {
        return ResponseEntity.ok(ngoProfileService.searchNGOProfiles(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NGOProfileResponseDTO> getNGOProfileById(@PathVariable String id) {
        return ResponseEntity.ok(ngoProfileService.getNGOProfileById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NGOProfileResponseDTO> createNGOProfile(
            @Valid @RequestBody NGOProfileRequestDTO request) {
        return ResponseEntity.ok(ngoProfileService.createNGOProfile(request));
    }

    @PutMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NGOProfileResponseDTO> updateNGOProfile(
            @Valid @RequestBody NGOProfileRequestDTO request) {
        return ResponseEntity.ok(ngoProfileService.updateNGOProfile(request));
    }

    @PatchMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NGOProfileResponseDTO> approveNGOProfile(@PathVariable String id) {
        return ResponseEntity.ok(ngoProfileService.approveNGOProfile(id));
    }

    @PatchMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NGOProfileResponseDTO> rejectNGOProfile(
            @PathVariable String id,
            @Valid @RequestBody NGOProfileRejectRequestDTO request) {
        return ResponseEntity.ok(ngoProfileService.rejectNGOProfile(id, request));
    }
}
