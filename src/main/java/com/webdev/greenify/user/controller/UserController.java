package com.webdev.greenify.user.controller;

import com.webdev.greenify.user.dto.ChangeUserRoleRequestDTO;
import com.webdev.greenify.user.dto.CtvEligibilityResponseDTO;
import com.webdev.greenify.user.dto.DemoteCtvRequestDTO;
import com.webdev.greenify.user.dto.SuspendUserRequestDTO;
import com.webdev.greenify.user.dto.UserAdminSummaryResponseDTO;
import com.webdev.greenify.user.dto.UserDetailResponseDTO;
import com.webdev.greenify.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserAdminSummaryResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsersForAdmin());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CTV', 'NGO')")
    public ResponseEntity<UserDetailResponseDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAdminSummaryResponseDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.findUserByIdForAdmin(id));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAdminSummaryResponseDTO> suspendUser(
            @PathVariable String id,
            @RequestBody @Valid SuspendUserRequestDTO request) {
        return ResponseEntity.ok(userService.suspendUser(id, request));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAdminSummaryResponseDTO> changeUserRole(
            @PathVariable String id,
            @RequestBody @Valid ChangeUserRoleRequestDTO request) {
        return ResponseEntity.ok(userService.changeUserRole(id, request));
    }

    @GetMapping("/me/ctv-eligibility")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<CtvEligibilityResponseDTO> checkCurrentUserCtvEligibility() {
        return ResponseEntity.ok(userService.checkCurrentUserCtvEligibility());
    }

    @PatchMapping("/me/ctv-upgrade")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<UserAdminSummaryResponseDTO> upgradeCurrentUserToCtv() {
        return ResponseEntity.ok(userService.upgradeCurrentUserToCtv());
    }

    @PatchMapping("/{id}/ctv-demotion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAdminSummaryResponseDTO> demoteCtvToUser(
            @PathVariable String id,
            @RequestBody @Valid DemoteCtvRequestDTO request) {
        return ResponseEntity.ok(userService.demoteCtvToUser(id, request));
    }
}
