package com.webdev.greenify.auth.service;

public interface OtpService {
    enum OtpType {
        REGISTRATION,
        FORGOT_PASSWORD
    }

    void processAndSendOtp(String identifier);
    void processAndSendOtp(String identifier, OtpType otpType);
    String verifyOtp(String identifier, String otp);
    String getIdentifierFromVerificationToken(String token);
    void clearVerificationToken(String token);
}
