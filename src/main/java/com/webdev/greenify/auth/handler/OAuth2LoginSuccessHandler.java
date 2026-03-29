package com.webdev.greenify.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webdev.greenify.auth.service.AuthenticationService;
import com.webdev.greenify.auth.dto.AuthenticationResponse;
import com.webdev.greenify.config.JwtProperties;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProperties jwtProperties;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        Optional<UserEntity> userOptional = userRepository.findByIdentifier(email);

        if (userOptional.isPresent()) {
            UserEntity userEntity = userOptional.get();
            try {
                String accessToken = authenticationService.generateToken(userEntity, jwtProperties.getExpiration());
                String refreshToken = authenticationService.generateToken(userEntity, jwtProperties.getRefreshTokenExpiration());

                AuthenticationResponse authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

                response.setContentType("application/json");
                new ObjectMapper().writeValue(response.getWriter(), authResponse);
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating token");
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UserEntity not found in database");
        }
    }

}
