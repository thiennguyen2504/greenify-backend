package com.webdev.greenify.auth.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.webdev.greenify.auth.service.OtpService;
import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CaffeineOtpService implements OtpService {

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Rate limiting: count of OTPs generated per identifier in 24h
    private final Cache<String, Integer> rateLimitCache;

    // Storing Hashed OTP. Identifier -> Hashed OTP
    private final Cache<String, String> otpHashCache;

    // Number of failed verification attempts. Identifier -> Integer
    private final Cache<String, Integer> otpAttemptCache;

    // Validation token map: Token -> Identifier
    private final Cache<String, String> verificationTokenCache;

    // Cooldown to prevent spamming OTP requests (e.g. 1 request per 60s)
    private final Cache<String, Boolean> otpCooldownCache;

    private static final int MAX_OTP_PER_DAY = 5;
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final long OTP_EXPIRE_MINUTES = 5;
    private static final long TOKEN_EXPIRE_MINUTES = 15;
    private static final long OTP_COOLDOWN_SECONDS = 15;

    public CaffeineOtpService(PasswordEncoder passwordEncoder, EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;

        this.rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build();

        this.otpHashCache = Caffeine.newBuilder()
                .expireAfterWrite(OTP_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();

        this.otpAttemptCache = Caffeine.newBuilder()
                .expireAfterWrite(OTP_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();

        this.verificationTokenCache = Caffeine.newBuilder()
                .expireAfterWrite(TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();

        this.otpCooldownCache = Caffeine.newBuilder()
                .expireAfterWrite(OTP_COOLDOWN_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void processAndSendOtp(String identifier) {
        // Rate limiting check
        Integer count = rateLimitCache.getIfPresent(identifier);
        if (count != null && count >= MAX_OTP_PER_DAY) {
            throw new AppException("Rate limit exceeded for OTP generation on this identifier",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        // Cooldown check
        if (otpCooldownCache.getIfPresent(identifier) != null) {
            throw new AppException("Please wait " + OTP_COOLDOWN_SECONDS + " seconds before requesting another OTP",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        // Generate 6 digit OTP
        String otp = generateNumericOtp(6);
        log.info("Generated OTP for {}: {}", identifier, otp); // in real environment, avoid saving plain OTP

        // Hash OTP and save
        String hashedOtp = passwordEncoder.encode(otp);
        otpHashCache.put(identifier, hashedOtp);
        otpAttemptCache.put(identifier, 0);

        // Update rate limit
        rateLimitCache.put(identifier, (count == null ? 0 : count) + 1);

        // Send OTP
        if (identifier.contains("@")) {
            // Simulated otp sending via email
            log.info("Simulating Email sent to {}: Your OTP is {}", identifier, otp);
        } else {
            // Simulated otp sending via sms
            log.info("Simulating SMS sent to {}: Your OTP is {}", identifier, otp);
        }

        // Put in cooldown cache after successful send
        otpCooldownCache.put(identifier, true);
    }

    @Override
    public String verifyOtp(String identifier, String otp) {
        String hashedOtp = otpHashCache.getIfPresent(identifier);

        if (hashedOtp == null) {
            throw new AppException("OTP has expired or hasn't been requested", HttpStatus.BAD_REQUEST);
        }

        Integer attempts = otpAttemptCache.getIfPresent(identifier);
        if (attempts != null && attempts >= MAX_OTP_ATTEMPTS) {
            otpHashCache.invalidate(identifier);
            otpAttemptCache.invalidate(identifier);
            throw new AppException("Too many wrong OTP attempts. Please request a new OTP",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        if (!passwordEncoder.matches(otp, hashedOtp)) {
            otpAttemptCache.put(identifier, (attempts == null ? 0 : attempts) + 1);
            throw new AppException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }

        // Verification success -> remove OTP to prevent reuse
        otpHashCache.invalidate(identifier);
        otpAttemptCache.invalidate(identifier);

        // Generate verification token and store
        String verificationToken = UUID.randomUUID().toString();
        verificationTokenCache.put(verificationToken, identifier);

        return verificationToken;
    }

    @Override
    public String getIdentifierFromVerificationToken(String token) {
        return verificationTokenCache.getIfPresent(token);
    }

    @Override
    public void clearVerificationToken(String token) {
        verificationTokenCache.invalidate(token);
    }

    private String generateNumericOtp(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
