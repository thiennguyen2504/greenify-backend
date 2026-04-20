package com.webdev.greenify.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webdev.greenify.config.UnsplashProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class UnsplashImageService {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int MAX_SEARCH_PAGES = 2;
    private static final int MAX_KEYWORD_CANDIDATES = 5;
    private static final String DEFAULT_API_BASE_URL = "https://api.unsplash.com";

    private final UnsplashProperties unsplashProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, List<String>> keywordCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> keywordIndex = new ConcurrentHashMap<>();
    private final AtomicBoolean missingCredentialsLogged = new AtomicBoolean(false);
    private final AtomicLong rateLimitedUntilEpochMs = new AtomicLong(0);
    private final AtomicBoolean rateLimitCooldownLogged = new AtomicBoolean(false);

    public UnsplashImageService(UnsplashProperties unsplashProperties) {
        this.unsplashProperties = unsplashProperties;
        this.restTemplate = buildRestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isEnabled() {
        return unsplashProperties.isEnabled();
    }

    public String getImageUrl(String keyword) {
        return getImageUrl(keyword, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public String getImageUrl(String keyword, int width, int height) {
        List<String> keywordCandidates = buildKeywordCandidates(keyword);
        String normalizedKeyword = keywordCandidates.get(0);

        String cachedUrl = getCachedImageUrl(keywordCandidates);
        if (cachedUrl != null) {
            return cachedUrl;
        }

        if (!canUseUnsplashApi()) {
            String fallback = getFallbackUrl(normalizedKeyword);
            mirrorCacheAcrossKeywords(keywordCandidates, List.of(fallback));
            return fallback;
        }

        try {
            List<String> fetchedUrls = fetchUsingKeywordCandidates(keywordCandidates, 1, width, height);
            if (fetchedUrls.isEmpty()) {
                throw new IllegalStateException("Unsplash API trả về kết quả rỗng");
            }

            mirrorCacheAcrossKeywords(keywordCandidates, fetchedUrls);
            return fetchedUrls.get(0);
        } catch (Exception ex) {
            log.warn("Unsplash getImageUrl failed for keyword '{}': {}. Falling back to Picsum.",
                    normalizedKeyword,
                    ex.getMessage());
            String fallback = getFallbackUrl(normalizedKeyword);
            mirrorCacheAcrossKeywords(keywordCandidates, List.of(fallback));
            return fallback;
        }
    }

    public List<String> getMultipleImageUrls(String keyword, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        List<String> keywordCandidates = buildKeywordCandidates(keyword);
        String normalizedKeyword = keywordCandidates.get(0);

        List<String> cachedUrls = getCachedImageUrls(keywordCandidates, count);
        if (cachedUrls != null) {
            return cachedUrls;
        }

        if (!canUseUnsplashApi()) {
            List<String> fallbackUrls = buildFallbackUrls(normalizedKeyword, count);
            mirrorCacheAcrossKeywords(keywordCandidates, fallbackUrls);
            return fallbackUrls;
        }

        try {
            List<String> fetchedUrls = fetchUsingKeywordCandidates(keywordCandidates, count, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            if (fetchedUrls.size() < count) {
                log.warn("Unsplash returned only {} image(s) for keyword '{}' (requested {}). Filling missing with Picsum fallback.",
                        fetchedUrls.size(),
                        normalizedKeyword,
                        count);
                List<String> fallbackUrls = buildFallbackUrls(normalizedKeyword, count - fetchedUrls.size());
                fetchedUrls.addAll(fallbackUrls);
            }

            mirrorCacheAcrossKeywords(keywordCandidates, fetchedUrls);
            List<String> merged = keywordCache.getOrDefault(normalizedKeyword, fetchedUrls);
            return getRoundRobinUrls(normalizedKeyword, merged, count);
        } catch (Exception ex) {
            log.warn("Unsplash getMultipleImageUrls failed for keyword '{}': {}. Falling back to Picsum.",
                    normalizedKeyword,
                    ex.getMessage());
            List<String> fallbackUrls = buildFallbackUrls(normalizedKeyword, count);
            mirrorCacheAcrossKeywords(keywordCandidates, fallbackUrls);
            return fallbackUrls;
        }
    }

    /**
     * Run one lightweight request to verify Unsplash connectivity and auth setup.
     */
    public void logConnectionDiagnostics() {
        log.info(
                "Unsplash diagnostics: enabled={}, baseUrl={}, timeoutMillis={}, searchPerPage={}, orientation={}, contentFilter={}, minWidth={}, minHeight={}, minLikes={}, accessKeyPresent={}, secretKeyPresent={}",
                unsplashProperties.isEnabled(),
                unsplashProperties.getBaseUrl(),
                unsplashProperties.getTimeoutMillis(),
                unsplashProperties.getSearchPerPage(),
                unsplashProperties.getOrientation(),
                unsplashProperties.getContentFilter(),
                unsplashProperties.getMinWidth(),
                unsplashProperties.getMinHeight(),
                unsplashProperties.getMinLikes(),
                hasText(unsplashProperties.getAccessKey()),
                hasText(unsplashProperties.getSecretKey()));

        if (!canUseUnsplashApi()) {
            log.warn("Unsplash diagnostics skipped because service is disabled or credentials are incomplete.");
            return;
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(normalizeBaseUrl())
                .path("/search/photos")
                .queryParam("query", "environment")
                .queryParam("page", 1)
                .queryParam("per_page", 1)
                .queryParam("order_by", "relevant")
                .queryParam("orientation", normalizeOrientation(unsplashProperties.getOrientation()))
                .queryParam("content_filter", normalizeContentFilter(unsplashProperties.getContentFilter()))
            .encode()
            .build()
                .toUri();

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(buildAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
            JsonNode root = hasText(response.getBody()) ? objectMapper.readTree(response.getBody()) : objectMapper.createObjectNode();

            int total = root.path("total").asInt(0);
            JsonNode resultsNode = root.path("results");
            int returned = resultsNode.isArray() ? resultsNode.size() : 0;

            String sampleUrl = null;
            if (resultsNode.isArray() && !resultsNode.isEmpty()) {
                sampleUrl = extractImageUrl(resultsNode.get(0), DEFAULT_WIDTH, DEFAULT_HEIGHT);
            }

            log.info("Unsplash healthcheck OK: status={}, total={}, returned={}, sampleUrl={}",
                    response.getStatusCode().value(),
                    total,
                    returned,
                    sampleUrl);
        } catch (RestClientResponseException ex) {
            log.error("Unsplash healthcheck failed: status={}, body={}",
                    ex.getRawStatusCode(),
                    truncateForLog(ex.getResponseBodyAsString(), 400));
        } catch (Exception ex) {
            log.error("Unsplash healthcheck failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Pre-fetch commonly used keywords so seed classes can reuse cached URLs.
     */
    public void warmupCache() {
        if (!canUseUnsplashApi()) {
            log.info("Unsplash is disabled or credential pair is missing; skipping warmup cache.");
            return;
        }

        List<String> keywords = List.of(
                "recycling environment",
                "volunteer cleanup",
                "tree planting green",
                "bicycle eco transport",
                "sunflower",
                "rose",
                "bamboo",
                "lotus",
                "plastic waste",
                "organic waste",
                "beach cleanup vietnam",
                "community event outdoor"
        );

        int warmed = 0;
        for (String keyword : keywords) {
            try {
                if (!canUseUnsplashApi()) {
                    log.warn("Unsplash warmup stopped early because API is temporarily unavailable.");
                    break;
                }

                getMultipleImageUrls(keyword, 2);
                warmed++;
            } catch (Exception ex) {
                log.warn("Warmup failed for keyword '{}': {}", keyword, ex.getMessage());
            }
        }

        log.info("Unsplash image cache warmed up with {} keywords", warmed);
    }

    private RestTemplate buildRestTemplate() {
        int timeout = (int) Math.max(1000, unsplashProperties.getTimeoutMillis());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return new RestTemplate(requestFactory);
    }

    private boolean canUseUnsplashApi() {
        if (!isEnabled()) {
            return false;
        }

        if (isRateLimitCooldownActive()) {
            return false;
        }

        boolean hasAccessKey = hasText(unsplashProperties.getAccessKey());
        boolean hasSecretKey = hasText(unsplashProperties.getSecretKey());

        // We validate both keys so project configuration always provides a full Unsplash credential pair.
        if (hasAccessKey && hasSecretKey) {
            return true;
        }

        if (missingCredentialsLogged.compareAndSet(false, true)) {
            log.warn("Unsplash API key pair is incomplete. Please set UNSPLASH_ACCESS_KEY and UNSPLASH_SECRET_KEY.");
        }

        return false;
    }

    private boolean isRateLimitCooldownActive() {
        long now = System.currentTimeMillis();
        long cooldownUntil = rateLimitedUntilEpochMs.get();

        if (cooldownUntil <= now) {
            rateLimitCooldownLogged.set(false);
            return false;
        }

        if (rateLimitCooldownLogged.compareAndSet(false, true)) {
            long remainingSeconds = Math.max(1, (cooldownUntil - now + 999) / 1000);
            log.warn("Unsplash is cooling down for {} second(s) due to rate limit. Using Picsum fallback.", remainingSeconds);
        }

        return true;
    }

    private void activateRateLimitCooldown(int statusCode, String responseBody) {
        long cooldownMillis = Math.max(30_000L, unsplashProperties.getRateLimitCooldownMillis());
        long cooldownUntil = System.currentTimeMillis() + cooldownMillis;
        long previous = rateLimitedUntilEpochMs.getAndUpdate(current -> Math.max(current, cooldownUntil));
        rateLimitCooldownLogged.set(false);

        if (cooldownUntil > previous) {
            log.warn(
                    "Unsplash rate limit detected (status={}, body={}). Cooldown for {} second(s).",
                    statusCode,
                    truncateForLog(responseBody, 180),
                    Math.max(1L, cooldownMillis / 1000));
        }
    }

    private boolean isRateLimitResponse(int statusCode, String responseBody) {
        if (statusCode == 429) {
            return true;
        }

        if (statusCode != 403) {
            return false;
        }

        String normalizedBody = responseBody == null ? "" : responseBody.toLowerCase();
        return normalizedBody.contains("rate limit") || normalizedBody.contains("too many requests");
    }

    private List<String> fetchUsingKeywordCandidates(List<String> keywordCandidates, int count, int width, int height) {
        int targetCount = Math.max(1, count);
        Set<String> collected = new LinkedHashSet<>();

        for (String candidate : keywordCandidates) {
            if (!canUseUnsplashApi()) {
                break;
            }

            int remaining = targetCount - collected.size();
            if (remaining <= 0) {
                break;
            }

            try {
                List<String> fetchedUrls = fetchFromUnsplashApi(candidate, remaining, width, height);
                collected.addAll(fetchedUrls);
            } catch (UnsplashApiException ex) {
                if (ex.isRateLimited()) {
                    throw new IllegalStateException("Unsplash API đã vượt giới hạn tần suất", ex);
                }

                if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
                    throw new IllegalStateException("Xác thực/phân quyền Unsplash API thất bại", ex);
                }

                log.debug(
                        "Unsplash search failed for candidate '{}' with status {}: {}",
                        candidate,
                        ex.getStatusCode(),
                        truncateForLog(ex.getResponseBody(), 200));
            } catch (Exception ex) {
                log.debug("Unsplash search failed for candidate '{}': {}", candidate, ex.getMessage());
            }
        }

        return new ArrayList<>(collected);
    }

    private List<String> fetchFromUnsplashApi(String keyword, int count, int width, int height) {
        int targetCount = Math.max(1, count);
        int perPage = normalizePerPage(unsplashProperties.getSearchPerPage());

        Set<String> strictMatches = new LinkedHashSet<>();
        Set<String> relaxedMatches = new LinkedHashSet<>();

        for (int page = 1; page <= MAX_SEARCH_PAGES && strictMatches.size() < targetCount; page++) {
            JsonNode root = searchPhotos(keyword, page, perPage);
            JsonNode resultsNode = root.path("results");
            if (!resultsNode.isArray() || resultsNode.isEmpty()) {
                break;
            }

            for (JsonNode photoNode : resultsNode) {
                String imageUrl = extractImageUrl(photoNode, width, height);
                if (!hasText(imageUrl)) {
                    continue;
                }

                if (passesQualityFilter(photoNode)) {
                    strictMatches.add(imageUrl);
                } else {
                    relaxedMatches.add(imageUrl);
                }

                if (strictMatches.size() >= targetCount) {
                    break;
                }
            }
        }

        if (strictMatches.size() < targetCount) {
            for (String relaxedUrl : relaxedMatches) {
                strictMatches.add(relaxedUrl);
                if (strictMatches.size() >= targetCount) {
                    break;
                }
            }
        }

        return new ArrayList<>(strictMatches);
    }

    private JsonNode searchPhotos(String keyword, int page, int perPage) {
        URI uri = UriComponentsBuilder.fromHttpUrl(normalizeBaseUrl())
                .path("/search/photos")
                .queryParam("query", keyword)
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .queryParam("order_by", "relevant")
                .queryParam("orientation", normalizeOrientation(unsplashProperties.getOrientation()))
                .queryParam("content_filter", normalizeContentFilter(unsplashProperties.getContentFilter()))
            .encode()
            .build()
                .toUri();

        HttpEntity<Void> requestEntity = new HttpEntity<>(buildAuthHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !hasText(response.getBody())) {
                throw new UnsplashApiException(
                        response.getStatusCode().value(),
                        "Unsplash API returned non-success response",
                        false,
                        null);
            }

            return objectMapper.readTree(response.getBody());
        } catch (RestClientResponseException ex) {
            int statusCode = ex.getRawStatusCode();
            String body = truncateForLog(ex.getResponseBodyAsString(), 300);
            boolean rateLimited = isRateLimitResponse(statusCode, body);
            if (rateLimited) {
                activateRateLimitCooldown(statusCode, body);
            }

            throw new UnsplashApiException(statusCode, body, rateLimited, ex);
        } catch (RestClientException ex) {
            throw new UnsplashApiException(0, "Cannot fetch images from Unsplash API", false, ex);
        } catch (Exception ex) {
            throw new UnsplashApiException(0, "Cannot parse Unsplash API response", false, ex);
        }
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Client-ID " + unsplashProperties.getAccessKey().trim());
        headers.set("Accept-Version", "v1");
        headers.set("Accept", "application/json");
        return headers;
    }

    private String extractImageUrl(JsonNode photoNode, int width, int height) {
        JsonNode urlsNode = photoNode.path("urls");

        String rawUrl = textOrNull(urlsNode.path("raw").asText(null));
        if (rawUrl != null) {
            return applyCropParams(rawUrl, width, height);
        }

        String regularUrl = textOrNull(urlsNode.path("regular").asText(null));
        if (regularUrl != null) {
            return regularUrl;
        }

        String fullUrl = textOrNull(urlsNode.path("full").asText(null));
        if (fullUrl != null) {
            return fullUrl;
        }

        return textOrNull(urlsNode.path("small").asText(null));
    }

    private String applyCropParams(String rawUrl, int width, int height) {
        try {
            return UriComponentsBuilder.fromUriString(rawUrl)
                    .replaceQueryParam("auto", "format")
                    .replaceQueryParam("fit", "crop")
                    .replaceQueryParam("crop", "entropy")
                    .replaceQueryParam("w", Math.max(1, width))
                    .replaceQueryParam("h", Math.max(1, height))
                    .replaceQueryParam("q", 80)
                    .build(true)
                    .toUriString();
        } catch (Exception ex) {
            return rawUrl;
        }
    }

    private boolean passesQualityFilter(JsonNode photoNode) {
        int width = photoNode.path("width").asInt(0);
        int height = photoNode.path("height").asInt(0);
        int likes = photoNode.path("likes").asInt(0);

        int minWidth = Math.max(1, unsplashProperties.getMinWidth());
        int minHeight = Math.max(1, unsplashProperties.getMinHeight());
        int minLikes = Math.max(0, unsplashProperties.getMinLikes());

        return width >= minWidth && height >= minHeight && likes >= minLikes;
    }

    private List<String> buildFallbackUrls(String keyword, int count) {
        List<String> fallbackUrls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            fallbackUrls.add(getFallbackUrl(keyword + "-" + i));
        }
        return fallbackUrls;
    }

    private void mirrorCacheAcrossKeywords(List<String> keywords, List<String> urls) {
        for (String keyword : keywords) {
            cacheUrls(keyword, urls);
        }
    }

    private String getCachedImageUrl(List<String> keywordCandidates) {
        for (String candidate : keywordCandidates) {
            List<String> cached = keywordCache.get(candidate);
            if (cached != null && !cached.isEmpty()) {
                return getRoundRobinUrl(candidate, cached);
            }
        }
        return null;
    }

    private List<String> getCachedImageUrls(List<String> keywordCandidates, int count) {
        for (String candidate : keywordCandidates) {
            List<String> cached = keywordCache.get(candidate);
            if (cached != null && !cached.isEmpty()) {
                return getRoundRobinUrls(candidate, cached, count);
            }
        }
        return null;
    }

    private List<String> buildKeywordCandidates(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        candidates.add(normalizedKeyword);
        candidates.add(normalizeKeyword(normalizedKeyword.replace('-', ' ')));

        if (keyword != null && keyword.contains(",")) {
            String[] rawParts = keyword.split(",");
            for (String rawPart : rawParts) {
                candidates.add(normalizeKeyword(rawPart));
            }
        }

        String withoutLocation = removeLocationHints(normalizedKeyword);
        if (hasText(withoutLocation)) {
            candidates.add(withoutLocation);
            candidates.add(keepFirstWords(withoutLocation, 3));
            candidates.add(keepFirstWords(withoutLocation, 2));
        }

        candidates.addAll(buildSemanticKeywordFallbacks(normalizedKeyword));

        List<String> limited = new ArrayList<>();
        for (String candidate : candidates) {
            if (!hasText(candidate)) {
                continue;
            }

            limited.add(candidate);
            if (limited.size() >= MAX_KEYWORD_CANDIDATES) {
                break;
            }
        }

        if (limited.isEmpty()) {
            return List.of("environment green");
        }

        return limited;
    }

    private String removeLocationHints(String keyword) {
        if (!hasText(keyword)) {
            return "";
        }

        return keyword
                .replaceAll("(?i)\\b(vietnam|viet\\s*nam|city|province|district|ward|urban|hcm|hanoi|saigon)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String keepFirstWords(String text, int maxWords) {
        if (!hasText(text) || maxWords <= 0) {
            return "";
        }

        String[] tokens = text.trim().split("\\s+");
        int limit = Math.min(tokens.length, maxWords);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (tokens[i].isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens[i]);
        }

        return builder.toString().trim();
    }

    private List<String> buildSemanticKeywordFallbacks(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        LinkedHashSet<String> fallbackKeywords = new LinkedHashSet<>();

        if (containsAny(lowerKeyword, "waste", "trash", "pollution", "recycling", "industrial", "chemical")) {
            fallbackKeywords.add("waste recycling");
            fallbackKeywords.add("environment cleanup");
        }

        if (containsAny(lowerKeyword, "bag", "bottle", "reusable", "plastic", "eco", "sustainable")) {
            fallbackKeywords.add("sustainable lifestyle");
            fallbackKeywords.add("reusable bag");
        }

        if (containsAny(lowerKeyword, "flower", "plant", "tree", "garden", "lotus", "rose", "sunflower", "tulip", "bamboo")) {
            fallbackKeywords.add("green plant");
        }

        if (containsAny(lowerKeyword, "community", "volunteer", "event", "cleanup")) {
            fallbackKeywords.add("community volunteer");
        }

        fallbackKeywords.add("environment green");

        return new ArrayList<>(fallbackKeywords);
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }

        return false;
    }

    private int normalizePerPage(int perPage) {
        return Math.max(5, Math.min(30, perPage));
    }

    private String normalizeBaseUrl() {
        String configuredBaseUrl = unsplashProperties.getBaseUrl();
        if (!hasText(configuredBaseUrl)) {
            return DEFAULT_API_BASE_URL;
        }

        String baseUrl = configuredBaseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return hasText(baseUrl) ? baseUrl : DEFAULT_API_BASE_URL;
    }

    private String normalizeOrientation(String orientation) {
        if (!hasText(orientation)) {
            return "landscape";
        }

        String normalized = orientation.trim().toLowerCase();
        return switch (normalized) {
            case "landscape", "portrait", "squarish" -> normalized;
            default -> "landscape";
        };
    }

    private String normalizeContentFilter(String contentFilter) {
        if (!hasText(contentFilter)) {
            return "high";
        }

        String normalized = contentFilter.trim().toLowerCase();
        return switch (normalized) {
            case "low", "high" -> normalized;
            default -> "high";
        };
    }

    private String textOrNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void cacheUrls(String keyword, List<String> urls) {
        List<String> validUrls = urls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .distinct()
                .toList();
        if (validUrls.isEmpty()) {
            return;
        }

        int cacheLimit = Math.max(1, unsplashProperties.getCacheSize());

        Set<String> mergedSet = new LinkedHashSet<>();
        List<String> existing = keywordCache.get(keyword);
        if (existing != null) {
            mergedSet.addAll(existing);
        }
        mergedSet.addAll(validUrls);

        List<String> merged = new ArrayList<>(mergedSet);
        if (merged.size() > cacheLimit) {
            merged = new ArrayList<>(merged.subList(merged.size() - cacheLimit, merged.size()));
        }

        keywordCache.put(keyword, merged);
        keywordIndex.computeIfAbsent(keyword, key -> new AtomicInteger(0));
    }

    private String getRoundRobinUrl(String keyword, List<String> cached) {
        AtomicInteger index = keywordIndex.computeIfAbsent(keyword, key -> new AtomicInteger(0));
        int next = Math.floorMod(index.getAndIncrement(), cached.size());
        return cached.get(next);
    }

    private List<String> getRoundRobinUrls(String keyword, List<String> cached, int count) {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            urls.add(getRoundRobinUrl(keyword, cached));
        }
        return urls;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "environment green";
        }
        return keyword.trim().replace(',', ' ').replaceAll("\\s+", " ");
    }

    private String getFallbackUrl(String keyword) {
        try {
            int hash = Math.abs(keyword.hashCode() % 1000);
            String seedText = sanitizeSeedText(keyword) + "-" + hash;
            return "https://picsum.photos/seed/" + seedText + "/" + DEFAULT_WIDTH + "/" + DEFAULT_HEIGHT;
        } catch (Exception ex) {
            return getPlaceholderUrl(keyword);
        }
    }

    private String getPlaceholderUrl(String keyword) {
        String encodedKeyword = URLEncoder.encode(normalizeKeyword(keyword), StandardCharsets.UTF_8);
        return "https://placehold.co/800x600/2d6a4f/white?text=" + encodedKeyword;
    }

    private String sanitizeSeedText(String keyword) {
        String cleaned = normalizeKeyword(keyword)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");

        if (cleaned.isBlank()) {
            return "greenify";
        }

        if (cleaned.length() > 80) {
            return cleaned.substring(0, 80);
        }

        return cleaned;
    }

    private String truncateForLog(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String cleaned = value.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        return cleaned.substring(0, maxLength) + "...";
    }

    private static final class UnsplashApiException extends RuntimeException {

        private final int statusCode;
        private final String responseBody;
        private final boolean rateLimited;

        private UnsplashApiException(int statusCode, String responseBody, boolean rateLimited, Throwable cause) {
            super("Unsplash API error status=" + statusCode + " body=" + responseBody, cause);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.rateLimited = rateLimited;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getResponseBody() {
            return responseBody;
        }

        private boolean isRateLimited() {
            return rateLimited;
        }
    }
}