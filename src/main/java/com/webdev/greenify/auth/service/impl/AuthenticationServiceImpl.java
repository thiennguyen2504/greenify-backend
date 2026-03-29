package com.webdev.greenify.auth.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.webdev.greenify.auth.dto.AuthenticationRequest;
import com.webdev.greenify.auth.dto.AuthenticationResponse;
import com.webdev.greenify.auth.dto.LogoutRequest;
import com.webdev.greenify.auth.dto.RefreshTokenRequest;
import com.webdev.greenify.auth.dto.RegisterRequest;
import com.webdev.greenify.auth.service.AuthenticationService;
import com.webdev.greenify.auth.service.TokenBlacklistService;
import com.webdev.greenify.common.exception.DuplicateResourceException;
import com.webdev.greenify.common.exception.InvalidTokenException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.common.exception.TokenException;
import com.webdev.greenify.config.JwtProperties;
import com.webdev.greenify.notification.service.EmailService;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtProperties jwtProperties;
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public void register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        Set<RoleEntity> roleEntities = new HashSet<>();
        if (request.getRoles() != null) {
            request.getRoles().forEach(roleName -> {
                RoleEntity roleEntity = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("RoleEntity not found: " + roleName));
                roleEntities.add(roleEntity);
            });
        }

        var user = UserEntity.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roleEntities)
                .build();
        var savedUser = repository.save(user);

        var jwtToken = generateToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstname(), jwtToken);
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("UserEntity not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        var jwtToken = generateToken(user);
        var refreshToken = generateRefreshToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistService.isRefreshTokenBlacklisted(refreshToken)) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        String userEmail = extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("UserEntity not found"));

            if (isTokenValid(refreshToken, user)) {
                tokenBlacklistService.blacklistRefreshToken(refreshToken);

                var accessToken = generateToken(user);
                var newRefreshToken = generateRefreshToken(user);

                return AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(newRefreshToken)
                        .build();
            }
        }
        throw new InvalidTokenException("Invalid refresh token");
    }

    @Override
    public void logout(LogoutRequest request) {
        String token = request.getToken();
        if (token != null) {
            tokenBlacklistService.blacklistAccessToken(token);
        }
    }

    @Override
    public void verifyUser(String token) {
        String email = extractUsername(token);
        if (email == null) {
            throw new InvalidTokenException("Invalid token");
        }
        UserEntity userEntity = repository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("UserEntity not found"));
        if (isTokenValid(token, userEntity)) {
            repository.save(userEntity);
        } else {
            throw new InvalidTokenException("Invalid token");
        }
    }

    public String generateToken(UserEntity userEntity) {
        return generateToken(userEntity, jwtProperties.getExpiration());
    }

    public String generateRefreshToken(UserEntity userEntity) {
        return generateToken(userEntity, jwtProperties.getRefreshTokenExpiration());
    }

    private String generateToken(UserEntity userEntity, long expiration) {
        try {
            JWSSigner signer = new MACSigner(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userEntity.getEmail())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expiration))
                    .claim("roles", userEntity.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList()));

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsBuilder.build());

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new TokenException("Error generating JWT", e);
        }
    }

    public String extractUsername(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserEntity userEntityDetails) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

            if (!signedJWT.verify(verifier)) {
                return false;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            String username = claims.getSubject();
            Date expiration = claims.getExpirationTime();

            return (username.equals(userEntityDetails.getEmail()) && expiration.after(new Date()));
        } catch (JOSEException | ParseException e) {
            return false;
        }
    }
}
