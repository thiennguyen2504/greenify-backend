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
import com.webdev.greenify.auth.dto.ForgotPasswordSetPasswordRequest;
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
import com.webdev.greenify.user.entity.UserManagementActionEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.enumeration.UserManagementActionType;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.user.repository.UserManagementActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String SUSPENDED_MESSAGE_PREFIX = "Tài khoản của bạn đã bị khóa. Lý do: ";
    private static final String DEFAULT_SUSPENDED_REASON = "Tài khoản đã bị khóa bởi quản trị viên";

    private final JwtProperties jwtProperties;
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final UserManagementActionRepository userManagementActionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;

    @Override
    public void sendOtp(SendOtpRequest request) {
        String normalizedIdentifier = normalizeIdentifier(request.getIdentifier());
        if (repository.findByIdentifier(normalizedIdentifier).isPresent()) {
            throw new DuplicateResourceException("Email hoặc số điện thoại đã được đăng ký");
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
    public void sendForgotPasswordOtp(SendOtpRequest request) {
        UserEntity user = findUserByIdentifier(request.getIdentifier());
        otpService.processAndSendOtp(resolveForgotPasswordOtpIdentifier(user));
    }

    @Override
    public VerifyOtpResponse verifyForgotPasswordOtp(VerifyOtpRequest request) {
        UserEntity user = findUserByIdentifier(request.getIdentifier());
        String token = otpService.verifyOtp(resolveForgotPasswordOtpIdentifier(user), request.getOtp());
        return VerifyOtpResponse.builder().verificationToken(token).build();
    }

    @Override
    @Transactional
    public void setForgotPassword(ForgotPasswordSetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AppException("Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST);
        }

        String identifier = otpService.getIdentifierFromVerificationToken(request.getVerificationToken());
        if (identifier == null) {
            throw new InvalidTokenException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }

        UserEntity user = repository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException("Mật khẩu mới không được trùng mật khẩu hiện tại", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
        otpService.clearVerificationToken(request.getVerificationToken());
    }

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST);
        }

        String identifier = otpService.getIdentifierFromVerificationToken(request.getVerificationToken());
        if (identifier == null) {
            throw new InvalidTokenException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }

        if (repository.findByIdentifier(identifier).isPresent()) {
            throw new DuplicateResourceException("Email hoặc số điện thoại đã được đăng ký");
        }

        RoleEntity role = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò USER"));

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
            .status(AccountStatus.ACTIVE)
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
    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String normalizedIdentifier = normalizeIdentifier(request.getIdentifier());

        UserEntity user = repository.findByIdentifier(normalizedIdentifier)
                .orElseThrow(() -> new BadCredentialsException("Thông tin đăng nhập không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Thông tin đăng nhập không chính xác");
        }

        validateAccountStatusForSignIn(user);

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
            return value.toLowerCase();
        }

        if (value.matches("^[0-9+().\\s-]+$")) {
            return normalizePhone(value);
        }

        return value.toLowerCase().replaceAll("\\s+", "");
    }

    private String normalizePhone(String phone) {
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = phoneUtil.parse(phone, "VN");

            if (!phoneUtil.isValidNumber(number)) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ");
            }

            return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);

        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Định dạng số điện thoại không hợp lệ", e);
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistService.isRefreshTokenBlacklisted(refreshToken)) {
            throw new InvalidTokenException("Refresh token đã bị thu hồi");
        }

        String userId = extractUserId(refreshToken);
        if (userId == null) {
            throw new InvalidTokenException("Refresh token không hợp lệ");
        }

        UserEntity user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        validateAccountStatusForSignIn(user);

        if (isTokenValid(refreshToken, user)) {
            tokenBlacklistService.blacklistRefreshToken(refreshToken);

            String accessToken = generateToken(user, jwtProperties.getExpiration());
            String newRefreshToken = generateToken(user, jwtProperties.getRefreshTokenExpiration());

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        }
        throw new InvalidTokenException("Refresh token không hợp lệ");
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
            throw new TokenException("Lỗi tạo JWT", e);
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

    private void validateAccountStatusForSignIn(UserEntity user) {
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            String reason = userManagementActionRepository
                    .findTopByUser_IdAndActionTypeOrderByCreatedAtDesc(user.getId(), UserManagementActionType.SUSPEND)
                    .map(UserManagementActionEntity::getReason)
                    .filter(this::hasText)
                    .orElse(DEFAULT_SUSPENDED_REASON);
            throw new AppException(SUSPENDED_MESSAGE_PREFIX + reason, HttpStatus.FORBIDDEN);
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException("Tài khoản chưa được kích hoạt", HttpStatus.FORBIDDEN);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private UserEntity findUserByIdentifier(String identifier) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        return repository.findByIdentifier(normalizedIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private String resolveForgotPasswordOtpIdentifier(UserEntity user) {
        if (!hasText(user.getEmail())) {
            throw new AppException("Tài khoản chưa liên kết email để nhận OTP", HttpStatus.BAD_REQUEST);
        }
        return user.getEmail().trim().toLowerCase();
    }
}
