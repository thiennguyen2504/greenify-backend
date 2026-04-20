package com.webdev.greenify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "unsplash")
public class UnsplashProperties {

    private String accessKey;
    private String secretKey;
    private String baseUrl = "https://api.unsplash.com";
    private long timeoutMillis = 3000;
    private boolean enabled = true;
    private int cacheSize = 20;
    private int searchPerPage = 30;
    private String orientation = "landscape";
    private String contentFilter = "high";
    private int minWidth = 1000;
    private int minHeight = 700;
    private int minLikes = 10;
    private long rateLimitCooldownMillis = 600000;
}
