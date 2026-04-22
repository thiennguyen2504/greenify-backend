package com.webdev.greenify.user.controller;

import com.webdev.greenify.user.dto.ChangePasswordRequestDTO;
import com.webdev.greenify.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserCredentialController {

    private final UserService userService;

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CTV', 'NGO')")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        userService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }
}
