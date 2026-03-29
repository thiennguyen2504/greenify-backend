package com.webdev.greenify.auth.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.webdev.greenify.auth.service.TokenBlacklistService;
import com.webdev.greenify.config.JwtProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CaffeineTokenBlacklistService implements TokenBlacklistService {

    private final Cache<String, Boolean> blacklistedAccessTokens;
    private final Cache<String, Boolean> blacklistedRefreshTokens;

    public CaffeineTokenBlacklistService(JwtProperties jwtProperties) {
        this.blacklistedAccessTokens = Caffeine.newBuilder()
                .expireAfterWrite(jwtProperties.getExpiration(), TimeUnit.MILLISECONDS)
                .build();
        this.blacklistedRefreshTokens = Caffeine.newBuilder()
                .expireAfterWrite(jwtProperties.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public void blacklistAccessToken(String token) {
        blacklistedAccessTokens.put(token, true);
    }

    @Override
    public void blacklistRefreshToken(String refreshToken) {
        blacklistedRefreshTokens.put(refreshToken, true);
    }

    @Override
    public boolean isAccessTokenBlacklisted(String token) {
        return blacklistedAccessTokens.getIfPresent(token) != null;
    }

    @Override
    public boolean isRefreshTokenBlacklisted(String token) {
        return blacklistedRefreshTokens.getIfPresent(token) != null;
    }
}
