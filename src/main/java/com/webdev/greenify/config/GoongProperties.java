package com.webdev.greenify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "goong")
public class GoongProperties {

    private String apiKey;
    private String geocodeUrl = "https://rsapi.goong.io/v2/geocode";
    private long timeoutMillis = 2500;
}
