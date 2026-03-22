package com.webdev.greenify.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.webdev.greenify.properties.JwtProperties;
import com.webdev.greenify.service.TokenBlacklistService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CaffeineTokenBlacklistService implements TokenBlacklistService {

    private final Cache<String, Boolean> blacklistedTokens;

    public CaffeineTokenBlacklistService(JwtProperties jwtProperties) {
        this.blacklistedTokens = Caffeine.newBuilder()
                .expireAfterWrite(jwtProperties.getExpiration(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public void blacklistToken(String token) {
        blacklistedTokens.put(token, true);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.getIfPresent(token) != null;
    }
}
