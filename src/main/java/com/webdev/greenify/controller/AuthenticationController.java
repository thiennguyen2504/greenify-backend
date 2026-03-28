package com.webdev.greenify.controller;

import com.webdev.greenify.dto.AuthenticationRequest;
import com.webdev.greenify.dto.AuthenticationResponse;
import com.webdev.greenify.dto.LogoutRequest;
import com.webdev.greenify.dto.RefreshTokenRequest;
import com.webdev.greenify.dto.RegisterRequest;
import com.webdev.greenify.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody @Valid RegisterRequest request) {
        service.register(request);
        return ResponseEntity.ok("UserEntity registered successfully. Please check your email to verify.");
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody @Valid AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(service.refreshToken(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        service.verifyUser(token);
        return ResponseEntity.ok("Account verified successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid LogoutRequest request) {
        service.logout(request);
        return ResponseEntity.ok("Logged out successfully");
    }
}
