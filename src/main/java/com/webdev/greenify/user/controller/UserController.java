package com.webdev.greenify.user.controller;

import com.webdev.greenify.user.dto.UpdateUserRolesRequestDTO;
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
    public ResponseEntity<List<UserDetailResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDetailResponseDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'NGO')")
    public ResponseEntity<UserDetailResponseDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDetailResponseDTO> updateUserRoles(@PathVariable String id, @RequestBody @Valid UpdateUserRolesRequestDTO request) {
        return ResponseEntity.ok(userService.updateUserRoles(id, request));
    }
}
