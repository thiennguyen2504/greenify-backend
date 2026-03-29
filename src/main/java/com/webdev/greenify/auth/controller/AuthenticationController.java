package com.webdev.greenify.auth.controller;

import com.webdev.greenify.auth.dto.AuthenticationRequest;
import com.webdev.greenify.auth.dto.AuthenticationResponse;
import com.webdev.greenify.auth.dto.LogoutRequest;
import com.webdev.greenify.auth.dto.RefreshTokenRequest;
import com.webdev.greenify.auth.dto.RegisterRequest;
import com.webdev.greenify.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
