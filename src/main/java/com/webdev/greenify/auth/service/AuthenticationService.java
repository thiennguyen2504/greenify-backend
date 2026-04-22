package com.webdev.greenify.auth.service;

import com.webdev.greenify.auth.dto.AuthenticationRequest;
import com.webdev.greenify.auth.dto.AuthenticationResponse;
import com.webdev.greenify.auth.dto.ForgotPasswordSetPasswordRequest;
import com.webdev.greenify.auth.dto.LogoutRequest;
import com.webdev.greenify.auth.dto.RefreshTokenRequest;
import com.webdev.greenify.auth.dto.RegisterRequest;
import com.webdev.greenify.auth.dto.SendOtpRequest;
import com.webdev.greenify.auth.dto.VerifyOtpRequest;
import com.webdev.greenify.auth.dto.VerifyOtpResponse;
import com.webdev.greenify.user.entity.UserEntity;

public interface AuthenticationService {
    void sendOtp(SendOtpRequest request);

    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    void sendForgotPasswordOtp(SendOtpRequest request);

    VerifyOtpResponse verifyForgotPasswordOtp(VerifyOtpRequest request);

    void setForgotPassword(ForgotPasswordSetPasswordRequest request);

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request, String authHeader);

    String generateToken(UserEntity userEntity, long expiration);
}
