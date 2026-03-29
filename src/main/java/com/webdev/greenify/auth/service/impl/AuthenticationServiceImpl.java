package com.webdev.greenify.auth.service.impl;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
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
import com.webdev.greenify.auth.dto.SendOtpRequest;
import com.webdev.greenify.auth.dto.VerifyOtpRequest;
import com.webdev.greenify.auth.dto.VerifyOtpResponse;
import com.webdev.greenify.auth.service.AuthenticationService;
import com.webdev.greenify.auth.service.OtpService;
import com.webdev.greenify.auth.service.TokenBlacklistService;
import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.DuplicateResourceException;
import com.webdev.greenify.common.exception.InvalidTokenException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.common.exception.TokenException;
import com.webdev.greenify.config.JwtProperties;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtProperties jwtProperties;
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;

    @Override
    public void sendOtp(SendOtpRequest request) {
        String normalizedIdentifier = normalizeIdentifier(request.getIdentifier());
        if (repository.findByIdentifier(normalizedIdentifier).isPresent()) {
            throw new DuplicateResourceException("Email or phone number already registered");
        }
        otpService.processAndSendOtp(normalizedIdentifier);
    }

    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedIdentifier = normalizeIdentifier(request.getIdentifier());
        String token = otpService.verifyOtp(normalizedIdentifier, request.getOtp());
        return VerifyOtpResponse.builder().verificationToken(token).build();
    }

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        String identifier = otpService.getIdentifierFromVerificationToken(request.getVerificationToken());
        if (identifier == null) {
            throw new InvalidTokenException("Invalid or expired verification token");
        }

        if (repository.findByIdentifier(identifier).isPresent()) {
            throw new DuplicateResourceException("Email or phone number already registered");
        }

        RoleEntity role = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role USER not found"));

        String email = null;
        String phone = null;
        if (identifier.contains("@")) {
            email = identifier;
        } else if (identifier.matches("^\\+[1-9]\\d{1,14}$")) {
            phone = identifier;
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .phoneNumber(phone)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(role))
                .status(com.webdev.greenify.user.enumeration.AccountStatus.ACTIVE)
                .build();
        repository.save(user);

        otpService.clearVerificationToken(request.getVerificationToken());
        String jwtToken = generateToken(user, jwtProperties.getExpiration());
        String refreshToken = generateToken(user, jwtProperties.getRefreshTokenExpiration());
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String normalizedIdentifier = normalizeIdentifier(request.getIdentifier());

        UserEntity user = repository.findByIdentifier(normalizedIdentifier)
                .orElseThrow(() -> new BadCredentialsException("Username or password is incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        String jwtToken = generateToken(user, jwtProperties.getExpiration());
        String refreshToken = generateToken(user, jwtProperties.getRefreshTokenExpiration());
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null)
            return null;
        String value = identifier.trim();

        if (value.contains("@")) {
            return identifier.toLowerCase();
        }

        if (value.matches("^[0-9+().\\s-]+$")) {
            return normalizePhone(value);
        }

        return identifier.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private String normalizePhone(String phone) {
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = phoneUtil.parse(phone, "VN");

            if (!phoneUtil.isValidNumber(number)) {
                throw new IllegalArgumentException("Invalid phone number");
            }

            return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);

        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Invalid phone format", e);
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistService.isRefreshTokenBlacklisted(refreshToken)) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        String userId = extractUserId(refreshToken);
        if (userId == null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        UserEntity user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (isTokenValid(refreshToken, user)) {
            tokenBlacklistService.blacklistRefreshToken(refreshToken);

            String accessToken = generateToken(user, jwtProperties.getExpiration());
            String newRefreshToken = generateToken(user, jwtProperties.getRefreshTokenExpiration());

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        }
        throw new InvalidTokenException("Invalid refresh token");
    }

    @Override
    public void logout(LogoutRequest request, String authHeader) {
        String refreshToken = request.getRefreshToken();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);

            tokenBlacklistService.blacklistAccessToken(accessToken);
        }
        tokenBlacklistService.blacklistRefreshToken(refreshToken);
    }

    @Override
    public String generateToken(UserEntity userEntity, long expiration) {
        try {
            JWSSigner signer = new MACSigner(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userEntity.getId())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expiration))
                    .claim("roles", userEntity.getRoles().stream().map(RoleEntity::getName).toList());

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsBuilder.build());

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new TokenException("Error generating JWT", e);
        }
    }

    public String extractUserId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserEntity user) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

            if (!signedJWT.verify(verifier)) {
                return false;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            String userId = claims.getSubject();
            Date expiration = claims.getExpirationTime();

            return (userId.equals(user.getId()) && expiration.after(new Date()));
        } catch (JOSEException | ParseException e) {
            return false;
        }
    }
}
