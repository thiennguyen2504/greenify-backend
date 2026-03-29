package com.webdev.greenify.auth.service;

import com.webdev.greenify.auth.dto.AuthenticationRequest;
import com.webdev.greenify.auth.dto.AuthenticationResponse;
import com.webdev.greenify.auth.dto.LogoutRequest;
import com.webdev.greenify.auth.dto.RefreshTokenRequest;
import com.webdev.greenify.auth.dto.RegisterRequest;







public interface AuthenticationService {
    void register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    void verifyUser(String token);

    void logout(LogoutRequest request);
}
