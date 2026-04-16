package com.webdev.greenify.greenaction.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.webdev.greenify.config.GoongProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class LocationSnapshotService {

    private static final String GOONG_STATUS_OK = "OK";

    private final GoongProperties goongProperties;
    private final RestTemplate restTemplate;

    public LocationSnapshotService(GoongProperties goongProperties) {
        this.goongProperties = goongProperties;
        int timeoutMillis = (int) Math.max(goongProperties.getTimeoutMillis(), 500L);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public String resolveLocationSnapshot(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        String fallbackLocation = formatCoordinates(latitude, longitude);
        if (goongProperties.getApiKey() == null || goongProperties.getApiKey().isBlank()) {
            log.warn("GOONG_API_KEY is missing. Use fallback coordinates for location snapshot.");
            return fallbackLocation;
        }

        String latlng = formatLatLng(latitude, longitude);
        URI uri = UriComponentsBuilder.fromUriString(goongProperties.getGeocodeUrl())
                .queryParam("latlng", latlng)
                .queryParam("api_key", goongProperties.getApiKey())
                .build(true)
                .toUri();

        try {
            GoongGeocodeResponse response = restTemplate.getForObject(uri, GoongGeocodeResponse.class);
            if (response == null || response.results() == null) {
                log.warn("Goong geocode response is empty for latlng={}", latlng);
                return fallbackLocation;
            }

            if (!GOONG_STATUS_OK.equalsIgnoreCase(response.status())) {
                log.warn("Goong geocode returned status {} for latlng={}", response.status(), latlng);
                return fallbackLocation;
            }

            return response.results().stream()
                    .map(GoongResult::formattedAddress)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(address -> !address.isEmpty())
                    .findFirst()
                    .orElse(fallbackLocation);
        } catch (RestClientException ex) {
            log.warn("Goong reverse geocode failed for latlng={}: {}", latlng, ex.getMessage());
            return fallbackLocation;
        }
    }

    private String formatLatLng(BigDecimal latitude, BigDecimal longitude) {
        return latitude.stripTrailingZeros().toPlainString()
                + ","
                + longitude.stripTrailingZeros().toPlainString();
    }

    private String formatCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return latitude.stripTrailingZeros().toPlainString()
                + ", "
                + longitude.stripTrailingZeros().toPlainString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoongGeocodeResponse(
            String status,
            List<GoongResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoongResult(
            @JsonProperty("formatted_address") String formattedAddress) {
    }
}
