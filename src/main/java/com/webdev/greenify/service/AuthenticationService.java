package com.webdev.greenify.service;

import com.webdev.greenify.dto.AuthenticationRequest;
import com.webdev.greenify.dto.AuthenticationResponse;
import com.webdev.greenify.dto.LogoutRequest;
import com.webdev.greenify.dto.RefreshTokenRequest;
import com.webdev.greenify.dto.RegisterRequest;

public interface AuthenticationService {
    void register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    void verifyUser(String token);

    void logout(LogoutRequest request);
}
