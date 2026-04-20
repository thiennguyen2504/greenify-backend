package com.webdev.greenify.station.service;

import com.webdev.greenify.station.dto.ProvinceResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvinceNormalizationService {

    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final AdministrativeDivisionService administrativeDivisionService;

    private volatile ProvinceLookup lookup = ProvinceLookup.empty();
    private volatile Instant loadedAt = Instant.EPOCH;
    private final Object reloadLock = new Object();

    public String normalizeProvinceName(String province) {
        if (province == null) {
            return null;
        }

        String trimmed = province.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        ProvinceLookup currentLookup = getLookup();

        String byName = currentLookup.byNormalizedText().get(toNormalizedTextKey(trimmed));
        if (byName != null) {
            return byName;
        }

        String byCodename = currentLookup.byCodename().get(toNormalizedCodename(trimmed));
        if (byCodename != null) {
            return byCodename;
        }

        return trimmed;
    }

    private ProvinceLookup getLookup() {
        Instant now = Instant.now();
        if (Duration.between(loadedAt, now).compareTo(CACHE_TTL) < 0
                && !lookup.byNormalizedText().isEmpty()) {
            return lookup;
        }

        synchronized (reloadLock) {
            now = Instant.now();
            if (Duration.between(loadedAt, now).compareTo(CACHE_TTL) < 0
                    && !lookup.byNormalizedText().isEmpty()) {
                return lookup;
            }

            ProvinceLookup loadedLookup = loadLookup();
            if (!loadedLookup.byNormalizedText().isEmpty()) {
                lookup = loadedLookup;
                loadedAt = now;
            } else {
                loadedAt = now;
            }

            return lookup;
        }
    }

    private ProvinceLookup loadLookup() {
        try {
            List<ProvinceResponseDTO> provinces = administrativeDivisionService.getProvinces();
            if (provinces == null || provinces.isEmpty()) {
                return ProvinceLookup.empty();
            }

            Map<String, String> byNormalizedText = new HashMap<>();
            Map<String, String> byCodename = new HashMap<>();

            for (ProvinceResponseDTO province : provinces) {
                if (province == null || province.getName() == null || province.getName().isBlank()) {
                    continue;
                }

                String canonicalName = province.getName().trim();
                byNormalizedText.putIfAbsent(toNormalizedTextKey(canonicalName), canonicalName);

                if (province.getCodename() != null && !province.getCodename().isBlank()) {
                    byCodename.putIfAbsent(toNormalizedCodename(province.getCodename()), canonicalName);
                }
            }

            return new ProvinceLookup(Map.copyOf(byNormalizedText), Map.copyOf(byCodename));
        } catch (Exception ex) {
            log.warn("Unable to load province list for normalization: {}", ex.getMessage());
            return ProvinceLookup.empty();
        }
    }

    private String toNormalizedTextKey(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return stripAdministrativePrefix(normalized);
    }

    private String toNormalizedCodename(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    private String stripAdministrativePrefix(String normalized) {
        String value = normalized;
        if (value.startsWith("thanh pho ")) {
            value = value.substring("thanh pho ".length());
        } else if (value.startsWith("tinh ")) {
            value = value.substring("tinh ".length());
        } else if (value.startsWith("tp ")) {
            value = value.substring("tp ".length());
        }
        return value.trim();
    }

    private record ProvinceLookup(
            Map<String, String> byNormalizedText,
            Map<String, String> byCodename) {
        private static ProvinceLookup empty() {
            return new ProvinceLookup(Map.of(), Map.of());
        }
    }
}