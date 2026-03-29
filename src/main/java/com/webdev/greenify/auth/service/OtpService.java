package com.webdev.greenify.auth.service;

public interface OtpService {
    void processAndSendOtp(String identifier);
    String verifyOtp(String identifier, String otp);
    String getIdentifierFromVerificationToken(String token);
    void clearVerificationToken(String token);
}
