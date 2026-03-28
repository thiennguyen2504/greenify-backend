package com.webdev.greenify.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.webdev.greenify.dto.AuthenticationResponse;
import com.webdev.greenify.entity.UserEntity;
import com.webdev.greenify.properties.JwtProperties;
import com.webdev.greenify.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            UserEntity userEntity = userOptional.get();
            try {
                String accessToken = generateToken(userEntity, jwtProperties.getExpiration());
                String refreshToken = generateToken(userEntity, jwtProperties.getRefreshTokenExpiration());

                AuthenticationResponse authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

                response.setContentType("application/json");
                new ObjectMapper().writeValue(response.getWriter(), authResponse);
            } catch (JOSEException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating token");
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UserEntity not found in database");
        }
    }

    private String generateToken(UserEntity userEntity, long expiration) throws JOSEException {
        JWSSigner signer = new MACSigner(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(userEntity.getEmail())
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + expiration))
                .claim("roleEntities", userEntity.getRoles()); // Add roleEntities as claim

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsBuilder.build());

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
