package com.webdev.greenify.auth.controller;

import com.webdev.greenify.auth.dto.AuthenticationRequest;
import com.webdev.greenify.auth.dto.AuthenticationResponse;
import com.webdev.greenify.auth.dto.LogoutRequest;
import com.webdev.greenify.auth.dto.RefreshTokenRequest;
import com.webdev.greenify.auth.dto.RegisterRequest;
import com.webdev.greenify.auth.dto.SendOtpRequest;
import com.webdev.greenify.auth.dto.VerifyOtpRequest;
import com.webdev.greenify.auth.dto.VerifyOtpResponse;
import com.webdev.greenify.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody @Valid SendOtpRequest request) {
        service.sendOtp(request);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        return ResponseEntity.ok(service.verifyOtp(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
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

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid LogoutRequest request, @RequestHeader("Authorization") String authHeader) {
        service.logout(request, authHeader);
        return ResponseEntity.ok("Logged out successfully");
    }
}
