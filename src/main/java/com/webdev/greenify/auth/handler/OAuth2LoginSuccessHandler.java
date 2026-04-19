package com.webdev.greenify.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webdev.greenify.auth.service.AuthenticationService;
import com.webdev.greenify.auth.dto.AuthenticationResponse;
import com.webdev.greenify.config.JwtProperties;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.UserManagementActionEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.enumeration.UserManagementActionType;
import com.webdev.greenify.user.repository.UserManagementActionRepository;
import com.webdev.greenify.user.repository.UserRepository;
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

    private static final String SUSPENDED_MESSAGE_PREFIX = "Tài khoản của bạn đã bị khóa. Lý do: ";
    private static final String DEFAULT_SUSPENDED_REASON = "Tài khoản đã bị khóa bởi quản trị viên";

    private final JwtProperties jwtProperties;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final UserManagementActionRepository userManagementActionRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        Optional<UserEntity> userOptional = userRepository.findByIdentifier(email);

        if (userOptional.isPresent()) {
            UserEntity userEntity = userOptional.get();
            if (userEntity.getStatus() == AccountStatus.SUSPENDED) {
                String reason = userManagementActionRepository
                        .findTopByUser_IdAndActionTypeOrderByCreatedAtDesc(
                                userEntity.getId(),
                                UserManagementActionType.SUSPEND)
                        .map(UserManagementActionEntity::getReason)
                        .filter(this::hasText)
                        .orElse(DEFAULT_SUSPENDED_REASON);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, SUSPENDED_MESSAGE_PREFIX + reason);
                return;
            }

            if (userEntity.getStatus() != AccountStatus.ACTIVE) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is not active");
                return;
            }

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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
