package com.webdev.greenify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey;
    private String model = "gemini-3.1-flash-lite-preview";
    private String apiUrl;
    private long timeoutMillis = 10000;
    private boolean enabled = false;
}
