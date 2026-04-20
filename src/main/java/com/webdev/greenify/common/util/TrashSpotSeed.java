package com.webdev.greenify.common.util;

import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.station.repository.WasteTypeRepository;
import com.webdev.greenify.station.service.ProvinceNormalizationService;
import com.webdev.greenify.trashspot.entity.TrashSpotEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotImageEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotResolveImageEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotResolveRequestEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotVerificationEntity;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import com.webdev.greenify.trashspot.enumeration.SeverityTier;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;
import com.webdev.greenify.trashspot.repository.TrashSpotRepository;
import com.webdev.greenify.trashspot.repository.TrashSpotResolveRequestRepository;
import com.webdev.greenify.trashspot.repository.TrashSpotVerificationRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrashSpotSeed {

    private static final long SEED_THRESHOLD = 3;

    private final TrashSpotRepository trashSpotRepository;
    private final TrashSpotVerificationRepository verificationRepository;
    private final TrashSpotResolveRequestRepository resolveRequestRepository;
    private final WasteTypeRepository wasteTypeRepository;
    private final UserRepository userRepository;
    private final ProvinceNormalizationService provinceNormalizationService;
    private final UnsplashImageService unsplashImageService;

    @Transactional
    public void seed() {
        if (trashSpotRepository.count() > SEED_THRESHOLD) {
            log.info("Skip TrashSpotSeed because trash spot count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            Map<String, WasteTypeEntity> wasteTypeByName = wasteTypeRepository.findAll().stream()
                    .collect(Collectors.toMap(w -> normalizeKey(w.getName()), Function.identity(), (left, right) -> left));

            UserEntity ngoUser = findNgoUser();
            UserEntity adminUser = findUserByUsername("admin");

            List<UserEntity> verifiers = loadUsers(List.of("ctv1", "ctv2", "ctv3", "user2", "user3", "user4"));
            if (verifiers.size() < 3) {
                log.warn("Skip TrashSpotSeed because not enough verifiers (need >= 3, found {})", verifiers.size());
                return;
            }

            seedSpot1(verifiers, wasteTypeByName);
            seedSpot2(ngoUser, verifiers, wasteTypeByName);
            seedSpot3(ngoUser, adminUser, verifiers, wasteTypeByName);
            seedSpot4(verifiers, wasteTypeByName);
            seedSpot5(verifiers, wasteTypeByName);
            seedSpot6(ngoUser, verifiers, wasteTypeByName);
            seedSpot7(verifiers, wasteTypeByName);
            seedSpot8(ngoUser, adminUser, verifiers, wasteTypeByName);

            log.info(
                    "TrashSpotSeed completed (spots={}, verifications={}, resolveRequests={})",
                    trashSpotRepository.count(),
                    verificationRepository.count(),
                    resolveRequestRepository.count());
        } catch (Exception e) {
            log.warn("TrashSpotSeed failed: {}", e.getMessage(), e);
        }
    }

    private void seedSpot1(List<UserEntity> verifiers, Map<String, WasteTypeEntity> wasteTypeByName) {
        try {
            UserEntity reporter = findUserByUsername("user1");
            if (reporter == null) {
                log.warn("Skip spot #1 because reporter user1 is missing");
                return;
            }

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Bãi rác tự phát đường Láng Hạ",
                    "Rác thải sinh hoạt tích tụ nhiều ngày, gây mùi hôi và ô nhiễm môi trường",
                    new BigDecimal("21.0245"),
                    new BigDecimal("105.8412"),
                    "Đường Láng Hạ, Hà Nội",
                    "Hà Nội",
                    TrashSpotStatus.VERIFIED,
                    3);

            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("plastic", "paper")));
            addVerifications(spot, verifiers.subList(0, 3), "Xác minh điểm rác tồn tại thực tế");
            recalculateHotScore(spot);

            spot = trashSpotRepository.save(spot);
            addSpotImages(spot, reporter, resolveSpotKeyword(spot), 2);
            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #1: {}", ex.getMessage());
        }
    }

    private void seedSpot2(UserEntity ngoUser, List<UserEntity> verifiers, Map<String, WasteTypeEntity> wasteTypeByName) {
        try {
            UserEntity reporter = findUserByUsername("user2");
            if (reporter == null) {
                log.warn("Skip spot #2 because reporter user2 is missing");
                return;
            }

            UserEntity effectiveNgo = ngoUser != null ? ngoUser : verifiers.get(0);

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Điểm đổ rác điện tử phố Hàng Bài",
                    "Nhiều linh kiện điện tử cũ bị tập kết sai quy định, cần đơn vị chuyên trách xử lý.",
                    new BigDecimal("21.0278"),
                    new BigDecimal("105.8520"),
                    "Phố Hàng Bài, Hà Nội",
                    "Hà Nội",
                    TrashSpotStatus.IN_PROGRESS,
                    3);

            spot.setAssignedNgo(effectiveNgo);
            spot.setClaimedAt(LocalDateTime.now().minusDays(2));
            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("hazardous", "plastic")));
            addVerifications(spot, verifiers.subList(0, 3), "Đã xác minh trước khi NGO nhận xử lý");
            recalculateHotScore(spot);

            spot = trashSpotRepository.save(spot);
            addSpotImages(spot, reporter, resolveSpotKeyword(spot), 2);
            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #2: {}", ex.getMessage());
        }
    }

    private void seedSpot3(
            UserEntity ngoUser,
            UserEntity adminUser,
            List<UserEntity> verifiers,
            Map<String, WasteTypeEntity> wasteTypeByName) {

        try {
            UserEntity reporter = findUserByUsername("user3");
            if (reporter == null) {
                log.warn("Skip spot #3 because reporter user3 is missing");
                return;
            }

            UserEntity effectiveNgo = ngoUser != null ? ngoUser : verifiers.get(0);

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Rác nổi hồ Tây khu vực Trúc Bạch",
                    "Rác nổi dày đặc ven hồ, ảnh hưởng mỹ quan và hệ sinh thái mặt nước.",
                    new BigDecimal("21.0505"),
                    new BigDecimal("105.8362"),
                    "Khu vực Trúc Bạch, Hà Nội",
                    "Hà Nội",
                    TrashSpotStatus.RESOLVED,
                    3);

            spot.setAssignedNgo(effectiveNgo);
            spot.setClaimedAt(LocalDateTime.now().minusDays(3));
            spot.setResolvedAt(LocalDateTime.now().minusDays(1));
            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("plastic", "paper")));
            addVerifications(spot, verifiers.subList(0, 3), "Xác minh trước khi xử lý");

                recalculateHotScore(spot);
                spot = trashSpotRepository.save(spot);
                addSpotImages(spot, reporter, resolveSpotKeyword(spot), 2);

            TrashSpotResolveRequestEntity resolveRequest = TrashSpotResolveRequestEntity.builder()
                    .trashSpot(spot)
                    .ngo(effectiveNgo)
                    .description("Đã thu gom toàn bộ rác nổi và vận chuyển đến điểm xử lý hợp lệ.")
                    .cleanedAt(LocalDateTime.now().minusDays(1))
                    .status(ResolveRequestStatus.APPROVED)
                    .reviewedBy(adminUser)
                    .reviewedAt(LocalDateTime.now().minusDays(1))
                    .build();
            addResolveImage(resolveRequest, spot.getId());
            spot.getResolveRequests().add(resolveRequest);

            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #3: {}", ex.getMessage());
        }
    }

    private void seedSpot4(List<UserEntity> verifiers, Map<String, WasteTypeEntity> wasteTypeByName) {
        try {
            UserEntity reporter = findUserByUsername("user4");
            if (reporter == null) {
                log.warn("Skip spot #4 because reporter user4 is missing");
                return;
            }

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Rác nổi kênh Nhiêu Lộc đoạn Quận 3",
                    "Nhiều túi nylon và chai nhựa trôi nổi, cần thêm xác minh cộng đồng.",
                    new BigDecimal("10.7790"),
                    new BigDecimal("106.6890"),
                    "Kênh Nhiêu Lộc, Quận 3",
                    "Thành phố Hồ Chí Minh",
                    TrashSpotStatus.PENDING_VERIFY,
                    1);

            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("plastic")));
            addVerifications(spot, verifiers.subList(0, 1), "Đã có 1 lượt xác minh ban đầu");
            recalculateHotScore(spot);

            spot = trashSpotRepository.save(spot);
            addSpotImages(spot, reporter, resolveSpotKeyword(spot), 2);
            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #4: {}", ex.getMessage());
        }
    }

    private void seedSpot5(List<UserEntity> verifiers, Map<String, WasteTypeEntity> wasteTypeByName) {
        try {
            UserEntity reporter = findUserByUsername("user5");
            if (reporter == null) {
                log.warn("Skip spot #5 because reporter user5 is missing");
                return;
            }

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Bãi rác tự phát khu Bình Chánh",
                    "Rác thải sinh hoạt và cồng kềnh tích tụ kéo dài, nguy cơ ô nhiễm cao.",
                    new BigDecimal("10.6890"),
                    new BigDecimal("106.6230"),
                    "Khu Bình Chánh, TP.HCM",
                    "Thành phố Hồ Chí Minh",
                    TrashSpotStatus.VERIFIED,
                    4);

            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("plastic", "paper", "hazardous")));
            addVerifications(spot, verifiers.subList(0, 4), "Điểm nóng đã được cộng đồng xác minh nhiều lần");
            recalculateHotScore(spot);

            if (spot.getSeverityTier() != SeverityTier.SEVERITY_HIGH) {
                spot.setSeverityTier(SeverityTier.SEVERITY_HIGH);
            }

            spot = trashSpotRepository.save(spot);
            addSpotImages(spot, reporter, resolveSpotKeyword(spot), 3);
            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #5: {}", ex.getMessage());
        }
    }

    private void seedSpot6(UserEntity ngoUser, List<UserEntity> verifiers, Map<String, WasteTypeEntity> wasteTypeByName) {
        try {
            UserEntity reporter = findUserByUsername("user6");
            if (reporter == null) {
                log.warn("Skip spot #6 because reporter user6 is missing");
                return;
            }

            UserEntity effectiveNgo = ngoUser != null ? ngoUser : verifiers.get(0);

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Rác thải công nghiệp KCN Tân Bình",
                    "Phát hiện nhiều bao bì hóa chất lẫn rác nhựa trong khu công nghiệp.",
                    new BigDecimal("10.8068"),
                    new BigDecimal("106.6402"),
                    "KCN Tân Bình, TP.HCM",
                    "Thành phố Hồ Chí Minh",
                    TrashSpotStatus.IN_PROGRESS,
                    3);

            spot.setAssignedNgo(effectiveNgo);
            spot.setClaimedAt(LocalDateTime.now().minusDays(2));
            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("hazardous", "plastic")));
            addVerifications(spot, verifiers.subList(0, 3), "Đủ điều kiện để NGO nhận xử lý");
            recalculateHotScore(spot);

            spot = trashSpotRepository.save(spot);
            addSpotImages(spot, reporter, resolveSpotKeyword(spot), 2);
            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #6: {}", ex.getMessage());
        }
    }

    private void seedSpot7(List<UserEntity> verifiers, Map<String, WasteTypeEntity> wasteTypeByName) {
        try {
            UserEntity reporter = findUserByUsername("user7");
            if (reporter == null) {
                log.warn("Skip spot #7 because reporter user7 is missing");
                return;
            }

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Vứt rác bừa bãi Công viên 23/9",
                    "Rác nhựa và thức ăn thừa sau hoạt động tập trung đông người.",
                    new BigDecimal("10.7715"),
                    new BigDecimal("106.6932"),
                    "Công viên 23/9, TP.HCM",
                    "Thành phố Hồ Chí Minh",
                    TrashSpotStatus.PENDING_VERIFY,
                    2);

            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("plastic", "paper")));
            addVerifications(spot, verifiers.subList(0, 2), "Đã có 2 xác minh cộng đồng");
            recalculateHotScore(spot);

            spot = trashSpotRepository.save(spot);
            addSpotImages(spot, reporter, resolveSpotKeyword(spot), 2);
            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #7: {}", ex.getMessage());
        }
    }

    private void seedSpot8(
            UserEntity ngoUser,
            UserEntity adminUser,
            List<UserEntity> verifiers,
            Map<String, WasteTypeEntity> wasteTypeByName) {

        try {
            UserEntity reporter = findUserByUsername("ctv1");
            if (reporter == null) {
                log.warn("Skip spot #8 because reporter ctv1 is missing");
                return;
            }

            UserEntity effectiveNgo = ngoUser != null ? ngoUser : verifiers.get(0);

            TrashSpotEntity spot = buildBaseSpot(
                    reporter,
                    "Điểm tập kết rác bất hợp pháp Thủ Đức",
                    "Điểm rác từng được xử lý nhưng đã tái xuất hiện trong tuần này.",
                    new BigDecimal("10.8570"),
                    new BigDecimal("106.7680"),
                    "Khu vực Thủ Đức, TP.HCM",
                    "Thành phố Hồ Chí Minh",
                    TrashSpotStatus.REOPENED,
                    2);

            spot.getWasteTypes().addAll(resolveWasteTypes(wasteTypeByName, List.of("plastic", "paper")));
            addVerifications(spot, verifiers.subList(0, 2), "Đã được xác minh lại sau khi tái xuất hiện");

                recalculateHotScore(spot);
                spot = trashSpotRepository.save(spot);
                addSpotImages(spot, reporter, resolveSpotKeyword(spot), 2);

            TrashSpotResolveRequestEntity resolveRequest = TrashSpotResolveRequestEntity.builder()
                    .trashSpot(spot)
                    .ngo(effectiveNgo)
                    .description("Đợt xử lý trước đã hoàn thành, tuy nhiên điểm rác xuất hiện trở lại.")
                    .cleanedAt(LocalDateTime.now().minusDays(12))
                    .status(ResolveRequestStatus.APPROVED)
                    .reviewedBy(adminUser)
                    .reviewedAt(LocalDateTime.now().minusDays(11))
                    .build();
            addResolveImage(resolveRequest, spot.getId());
            spot.getResolveRequests().add(resolveRequest);

            trashSpotRepository.save(spot);
        } catch (Exception ex) {
            log.warn("Failed seeding spot #8: {}", ex.getMessage());
        }
    }

    private TrashSpotEntity buildBaseSpot(
            UserEntity reporter,
            String name,
            String description,
            BigDecimal latitude,
            BigDecimal longitude,
            String location,
            String province,
            TrashSpotStatus status,
            int verificationCount) {

        return TrashSpotEntity.builder()
                .reporter(reporter)
                .name(name)
                .description(description)
                .latitude(latitude)
                .longitude(longitude)
                .location(location)
                .province(normalizeProvince(province))
                .status(status)
                .verificationCount(verificationCount)
                .build();
    }

    private String resolveSpotKeyword(TrashSpotEntity spot) {
        String wasteTypeName = spot.getWasteTypes().stream()
                .findFirst()
                .map(WasteTypeEntity::getName)
                .orElse(null);
        return UnsplashKeywordMapper.getTrashSpotKeyword(spot.getProvince(), wasteTypeName);
    }

    private void addSpotImages(TrashSpotEntity spot, UserEntity reporter, String keyword, int count) {
        List<String> imageUrls = unsplashImageService.getMultipleImageUrls(keyword, count);
        for (String imageUrl : imageUrls) {
            TrashSpotImageEntity image = TrashSpotImageEntity.builder()
                    .trashSpot(spot)
                    .bucketName("unsplash-cdn")
                    .objectKey("unsplash/trashspot/" + spot.getId() + "/" + UUID.randomUUID())
                    .imageUrl(imageUrl)
                    .uploadedBy(reporter)
                    .build();
            spot.getImages().add(image);
        }
    }

    private void addResolveImage(TrashSpotResolveRequestEntity resolveRequest, String spotId) {
        String imageUrl = unsplashImageService.getImageUrl("clean-street,clean-environment,after-cleanup");
        TrashSpotResolveImageEntity image = TrashSpotResolveImageEntity.builder()
                .resolveRequest(resolveRequest)
                .bucketName("unsplash-cdn")
                .objectKey("unsplash/trashspot/" + spotId + "/resolve/" + UUID.randomUUID())
                .imageUrl(imageUrl)
                .build();
        resolveRequest.getImages().add(image);
    }

    private void addVerifications(TrashSpotEntity spot, List<UserEntity> verifiers, String note) {
        for (UserEntity verifier : verifiers) {
            TrashSpotVerificationEntity verification = TrashSpotVerificationEntity.builder()
                    .trashSpot(spot)
                    .verifier(verifier)
                    .note(note)
                    .build();
            spot.getVerifications().add(verification);
        }
    }

    private Set<WasteTypeEntity> resolveWasteTypes(Map<String, WasteTypeEntity> wasteTypeByName, List<String> aliases) {
        Set<WasteTypeEntity> resolved = new HashSet<>();

        for (String alias : aliases) {
            WasteTypeEntity wasteType = wasteTypeByName.get(normalizeKey(alias));
            if (wasteType != null) {
                resolved.add(wasteType);
            }
        }

        if (resolved.isEmpty()) {
            log.warn("Could not resolve waste types for aliases {}", aliases);
        }

        return resolved;
    }

    private String normalizeProvince(String province) {
        String normalized = provinceNormalizationService.normalizeProvinceName(province);
        if (normalized == null || normalized.isBlank()) {
            return province;
        }
        return normalized;
    }

    private String normalizeKey(String input) {
        if (input == null) {
            return "";
        }

        String value = input.trim().toLowerCase();
        value = value.replace("đ", "d")
                .replace("á", "a")
                .replace("à", "a")
                .replace("ả", "a")
                .replace("ã", "a")
                .replace("ạ", "a")
                .replace("â", "a")
                .replace("ă", "a")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ẻ", "e")
                .replace("ẽ", "e")
                .replace("ẹ", "e")
                .replace("ê", "e")
                .replace("í", "i")
                .replace("ì", "i")
                .replace("ỉ", "i")
                .replace("ĩ", "i")
                .replace("ị", "i")
                .replace("ó", "o")
                .replace("ò", "o")
                .replace("ỏ", "o")
                .replace("õ", "o")
                .replace("ọ", "o")
                .replace("ô", "o")
                .replace("ơ", "o")
                .replace("ú", "u")
                .replace("ù", "u")
                .replace("ủ", "u")
                .replace("ũ", "u")
                .replace("ụ", "u")
                .replace("ư", "u")
                .replace("ý", "y")
                .replace("ỳ", "y")
                .replace("ỷ", "y")
                .replace("ỹ", "y")
                .replace("ỵ", "y");

        return switch (value) {
            case "plastic", "nhua" -> "nhua";
            case "paper", "giay" -> "giay";
            case "hazardous", "nguy hai" -> "nguy hai";
            default -> value;
        };
    }

    private void recalculateHotScore(TrashSpotEntity spot) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = spot.getCreatedAt() != null ? spot.getCreatedAt() : now;

        double ageInHours = ChronoUnit.HOURS.between(createdAt, now);
        double decayFactor = 1.0 / (1.0 + ageInHours / 48.0);
        int verificationCount = spot.getVerificationCount() != null ? spot.getVerificationCount() : 0;
        double hotScore = (verificationCount * 3.0 + 1.0) * decayFactor;

        spot.setHotScore(BigDecimal.valueOf(hotScore).setScale(4, RoundingMode.HALF_UP));
        spot.setSeverityTier(resolveSeverityTier(hotScore));
    }

    private SeverityTier resolveSeverityTier(double hotScore) {
        if (hotScore >= 10.0) {
            return SeverityTier.SEVERITY_HIGH;
        }
        if (hotScore >= 5.0) {
            return SeverityTier.SEVERITY_MEDIUM;
        }
        return SeverityTier.SEVERITY_LOW;
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .or(() -> userRepository.findByIdentifier(username + "@greenify.vn"))
                .orElse(null);
    }

    private UserEntity findNgoUser() {
        UserEntity ngo = findUserByUsername("ngo1");
        if (ngo != null) {
            return ngo;
        }

        ngo = findUserByUsername("ngo_tester");
        if (ngo != null) {
            return ngo;
        }

        return userRepository.findByIdentifier("ngo@example.com").orElse(null);
    }

    private List<UserEntity> loadUsers(List<String> usernames) {
        List<UserEntity> users = new ArrayList<>();
        for (String username : usernames) {
            UserEntity user = findUserByUsername(username);
            if (user != null) {
                users.add(user);
            }
        }

        return users.stream().filter(Objects::nonNull).distinct().toList();
    }
}
