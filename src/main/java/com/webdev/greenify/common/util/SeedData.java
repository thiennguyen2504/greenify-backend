package com.webdev.greenify.common.util;

import com.webdev.greenify.file.enumeration.EventImageType;
import com.webdev.greenify.garden.entity.SeedEntity;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import com.webdev.greenify.garden.repository.SeedRepository;
import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.station.repository.WasteTypeRepository;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.NGOProfileEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.enumeration.NGOProfileStatus;
import com.webdev.greenify.user.repository.NGOProfileRepository;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;
import com.webdev.greenify.voucher.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class SeedData implements CommandLineRunner {

        private static final String SEEDED_PASSWORD = "password123";
        private static final String STAGE_SEED_IMAGE_URL = "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/hat%202.png?updatedAt=1776237615245";
        private static final String STAGE_SPROUT_IMAGE_URL = "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/nay%20mam.png?updatedAt=1776237615268";
        private static final String STAGE_GROWING_IMAGE_URL = "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/leaf-plant.png?updatedAt=1776237615257";

        private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final GreenActionTypeRepository greenActionTypeRepository;
        private final WasteTypeRepository wasteTypeRepository;
        private final SeedRepository seedRepository;
        private final VoucherTemplateRepository voucherTemplateRepository;
        private final JdbcTemplate jdbcTemplate;
        private final NGOProfileRepository ngoProfileRepository;
        private final UserSeed userSeed;
        private final PostSeed postSeed;
        private final GardenSeed gardenSeed;
        private final PointWalletSeed pointWalletSeed;
        private final StreakSeed streakSeed;
        private final TrashSpotSeed trashSpotSeed;
        private final LeaderboardSeed leaderboardSeed;
        private final EventSeed eventSeed;
                private final NGOSeed ngoSeed;
                private final RegistrationSeed registrationSeed;
        private final UnsplashImageService unsplashImageService;
        private final GreenActionPostRepository greenActionPostRepository;
        private final EventRepository eventRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

                // Backfill legacy null soft-delete flags before any entity mapping occurs.
                normalizeSoftDeleteFlags();

                if (unsplashImageService.isEnabled()) {
                        unsplashImageService.logConnectionDiagnostics();
                        unsplashImageService.warmupCache();
                }

        // Ensure roleEntities exist
        RoleEntity adminRoleEntity = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ADMIN").build()));

        RoleEntity userRoleEntity = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("USER").build()));

        RoleEntity ctvRoleEntity = roleRepository.findByName("CTV")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("CTV").build()));

        RoleEntity ngoRoleEntity = roleRepository.findByName("NGO")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("NGO").build()));

        // Create Admin UserEntity
        if (userRepository.findByIdentifier("admin@example.com").isEmpty()) {
            Set<RoleEntity> adminRoleEntities = new HashSet<>();
            adminRoleEntities.add(adminRoleEntity);
            adminRoleEntities.add(userRoleEntity);

            userRepository.save(UserEntity.builder()
                    .email("admin@example.com")
                    .username("admin")
                    .password(passwordEncoder.encode("password123"))
                    .roles(adminRoleEntities)
                    .status(AccountStatus.ACTIVE)
                    .build());
        }

        // Create Normal UserEntity
        if (userRepository.findByIdentifier("user@example.com").isEmpty()) {
            Set<RoleEntity> userRoleEntities = new HashSet<>();
            userRoleEntities.add(userRoleEntity);

            userRepository.save(UserEntity.builder()
                    .email("user@example.com")
                    .username("user")
                    .password(passwordEncoder.encode("password123"))
                    .roles(userRoleEntities)
                    .status(AccountStatus.ACTIVE)
                    .build());
        }

        // Create CTV users for review workflows
        createCtvUserIfMissing("ctv1@example.com", "ctv1", ctvRoleEntity, userRoleEntity);
        createCtvUserIfMissing("ctv2@example.com", "ctv2", ctvRoleEntity, userRoleEntity);
        createCtvUserIfMissing("ctv3@example.com", "ctv3", ctvRoleEntity, userRoleEntity);

        // Create NGO UserEntity
        if (userRepository.findByIdentifier("ngo@example.com").isEmpty()) {
            Set<RoleEntity> ngoRoleEntities = new HashSet<>();
            ngoRoleEntities.add(ngoRoleEntity);
            ngoRoleEntities.add(userRoleEntity);

            userRepository.save(UserEntity.builder()
                    .email("ngo@example.com")
                    .username("ngo_tester")
                    .password(passwordEncoder.encode("password123"))
                    .roles(ngoRoleEntities)
                    .status(AccountStatus.ACTIVE)
                    .build());
        }

        // Seed Green Action Types
        seedGreenActionTypes();

        // Seed Waste Types
        seedWasteTypes();

        // Seed vouchers for garden rewards
        Map<PlantCycleType, VoucherTemplateEntity> rewardVoucherByCycle = seedGardenRewardVouchers();

        // Seed Garden Seeds
        seedGardenSeeds(rewardVoucherByCycle);

        // Seed NGO Profile for tester
        seedNGOProfiles();

        // Additional demo seeds with dependency-aware order
        userSeed.seed();
        postSeed.seed();
        ngoSeed.seed();
        eventSeed.seed();
        registrationSeed.seed();
        gardenSeed.seed();
        pointWalletSeed.seed();
        streakSeed.seed();
        trashSpotSeed.seed();
        leaderboardSeed.seed();

                logSeededSampleImageUrls();
    }

        private void logSeededSampleImageUrls() {
                String samplePostImageUrl = greenActionPostRepository.findAll().stream()
                                .map(post -> post.getPostImage() != null ? post.getPostImage().getImageUrl() : null)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);

                String sampleEventThumbnailUrl = eventRepository.findAll().stream()
                                .flatMap(event -> event.getImages().stream())
                                .filter(image -> image.getImageType() == EventImageType.THUMBNAIL)
                                .map(image -> image.getImageUrl())
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);

                if (samplePostImageUrl != null) {
                        log.info("Sample seeded post image URL: {}", samplePostImageUrl);
                }
                if (sampleEventThumbnailUrl != null) {
                        log.info("Sample seeded event thumbnail URL: {}", sampleEventThumbnailUrl);
                }
        }

    private void seedNGOProfiles() {
        UserEntity ngoUser = userRepository.findByIdentifier("ngo@example.com").orElse(null);
        if (ngoUser != null && ngoProfileRepository.findByUserId(ngoUser.getId()).isEmpty()) {
            NGOProfileEntity profile = NGOProfileEntity.builder()
                    .orgName("Hành Tinh Xanh Foundation")
                    .representativeName("Nguyễn Văn A")
                    .hotline("0912345678")
                    .contactEmail("contact@hanhtinhxanh.org")
                    .description("Tổ chức phi lợi nhuận vì môi trường xanh.")
                    .status(NGOProfileStatus.VERIFIED)
                    .user(ngoUser)
                    .rejectedCount(0)
                    .build();
            ngoProfileRepository.save(profile);
            log.info("Seeded NGO profile for user: {}", ngoUser.getEmail());
        }
    }


        private void normalizeSoftDeleteFlags() {
                try {
                        List<String> tables = jdbcTemplate.queryForList("""
                                        SELECT DISTINCT table_name
                                        FROM information_schema.columns
                                        WHERE lower(column_name) = 'is_deleted'
                                          AND lower(table_schema) NOT IN ('information_schema', 'pg_catalog')
                                        """, String.class);

                        for (String table : tables) {
                                int updatedRows = jdbcTemplate.update(
                                                "UPDATE " + table + " SET is_deleted = FALSE WHERE is_deleted IS NULL");
                                if (updatedRows > 0) {
                                        log.info("Backfilled {} rows in {}.is_deleted", updatedRows, table);
                                }
                        }
                } catch (Exception ex) {
                        log.warn("Could not normalize soft-delete flags before seeding", ex);
                }
    }

        private void createCtvUserIfMissing(String email, String username, RoleEntity ctvRoleEntity, RoleEntity userRoleEntity) {
                Set<RoleEntity> ctvRoleEntities = new HashSet<>();
                ctvRoleEntities.add(ctvRoleEntity);
                ctvRoleEntities.add(userRoleEntity);

                UserEntity existingCtvUser = userRepository.findByIdentifier(email).orElse(null);
                if (existingCtvUser == null) {
                        userRepository.save(UserEntity.builder()
                                        .email(email)
                                        .username(username)
                                        .password(passwordEncoder.encode(SEEDED_PASSWORD))
                                        .roles(ctvRoleEntities)
                                        .status(AccountStatus.ACTIVE)
                                        .build());
                        return;
                }

                existingCtvUser.setUsername(username);
                existingCtvUser.setPassword(passwordEncoder.encode(SEEDED_PASSWORD));
                existingCtvUser.setStatus(AccountStatus.ACTIVE);
                existingCtvUser.getRoles().addAll(ctvRoleEntities);
                userRepository.save(existingCtvUser);
        }

    private void seedGreenActionTypes() {
        if (greenActionTypeRepository.count() == 0) {
            List<GreenActionTypeEntity> actionTypes = new ArrayList<>();

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Phân loại rác")
                    .actionName("Phân loại rác tại nhà")
                    .suggestedPoints(new BigDecimal("5"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Phân loại rác")
                    .actionName("Phân loại rác tại nơi làm việc/trường học")
                    .suggestedPoints(new BigDecimal("6"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tái chế")
                    .actionName("Thu gom giấy/nhựa/lon để tái chế")
                    .suggestedPoints(new BigDecimal("5"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tái chế")
                    .actionName("Mang rác tái chế đến điểm thu gom")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Giảm nhựa dùng một lần")
                    .actionName("Sử dụng bình nước cá nhân")
                    .suggestedPoints(new BigDecimal("2"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Giảm nhựa dùng một lần")
                    .actionName("Mang hộp/cốc cá nhân khi mua đồ ăn, thức uống")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Giảm nhựa dùng một lần")
                    .actionName("Từ chối túi nilon/ống hút nhựa")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tiết kiệm tài nguyên")
                    .actionName("Tắt điện khi không sử dụng")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tiết kiệm tài nguyên")
                    .actionName("Tiết kiệm nước trong sinh hoạt hằng ngày")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Di chuyển xanh")
                    .actionName("Đi bộ/đạp xe cho quãng đường ngắn")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Di chuyển xanh")
                    .actionName("Sử dụng phương tiện công cộng")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Dọn dẹp môi trường")
                    .actionName("Nhặt rác tại khu vực công cộng")
                    .suggestedPoints(new BigDecimal("8"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Dọn dẹp môi trường")
                    .actionName("Dọn dẹp khu dân cư nơi sinh sống")
                    .suggestedPoints(new BigDecimal("6"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Mảng xanh")
                    .actionName("Trồng cây/trồng hoa")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Mảng xanh")
                    .actionName("Chăm sóc cây xanh thường xuyên")
                    .suggestedPoints(new BigDecimal("2"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tái sử dụng")
                    .actionName("Tái sử dụng vật dụng thay vì vứt bỏ")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tiêu dùng xanh")
                    .actionName("Mua sản phẩm thân thiện môi trường")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tham gia cộng đồng")
                    .actionName("Tham gia sự kiện môi trường do ứng dụng/NGO tổ chức")
                    .suggestedPoints(new BigDecimal("10"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Lan tỏa nhận thức xanh")
                    .actionName("Lan tỏa thông điệp môi trường gắn với hành động thực tế")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Sáng kiến xanh")
                    .actionName("Tổ chức/khởi xướng các hoạt động xanh quy mô nhỏ")
                    .suggestedPoints(new BigDecimal("9"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Phản ánh môi trường")
                    .actionName("Báo cáo điểm đổ rác trái phép/điểm ô nhiễm")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Phản ánh môi trường")
                    .actionName("Gửi phản ánh môi trường kèm hình ảnh")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tái chế sáng tạo")
                    .actionName("Tự làm sản phẩm từ vật liệu tái chế")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Sống xanh tại nhà")
                    .actionName("Ủ rác hữu cơ tại nhà")
                    .suggestedPoints(new BigDecimal("8"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Đóng góp cộng đồng")
                    .actionName("Kiểm duyệt bài đăng với vai trò CTV")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Đóng góp cộng đồng")
                    .actionName("Duyệt bài hợp lệ với vai trò CTV")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            greenActionTypeRepository.saveAll(actionTypes);
            log.info("Seeded {} green action types", actionTypes.size());
        }
    }

    private void seedWasteTypes() {
        if (wasteTypeRepository.count() == 0) {
            List<WasteTypeEntity> wasteTypes = new ArrayList<>();

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Nhựa")
                    .description("Chai nhựa, hộp đựng, túi nilon và các vật dụng nhựa sinh hoạt.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Giấy")
                    .description("Báo giấy, sách, bìa carton và giấy văn phòng.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Kim loại")
                    .description("Lon nhôm, sắt vụn, nhôm và đồng.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Thủy tinh")
                    .description("Chai, lọ thủy tinh và đồ thủy tinh vỡ.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Hữu cơ")
                    .description("Thức ăn thừa, vỏ trái cây, rau củ và rác vườn.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Nguy hại")
                    .description("Pin cũ, bóng đèn huỳnh quang và vỏ hộp hóa chất.")
                    .build());

            wasteTypeRepository.saveAll(wasteTypes);
            log.info("Seeded {} waste types", wasteTypes.size());
        }
    }

    private Map<PlantCycleType, VoucherTemplateEntity> seedGardenRewardVouchers() {
        Map<String, VoucherTemplateEntity> existingByName = new HashMap<>();
        for (VoucherTemplateEntity template : voucherTemplateRepository.findAll()) {
            existingByName.put(template.getName(), template);
        }

        LocalDateTime validUntil = LocalDateTime.now().plusYears(3);
        Map<PlantCycleType, VoucherTemplateEntity> rewardVoucherByCycle = new EnumMap<>(PlantCycleType.class);

        rewardVoucherByCycle.put(
                PlantCycleType.EASY,
                upsertRewardVoucherTemplate(
                        existingByName,
                        "Giảm 20% Highlands Coffee",
                        "Highlands Coffee",
                        new BigDecimal("30"),
                        800,
                        validUntil,
                        "Voucher giảm 20% tối đa 30.000đ tại Highlands Coffee cho cây chu kỳ dễ (30-45 ngày).",
                        "Áp dụng cho hóa đơn từ 80.000đ, không cộng dồn với chương trình khuyến mãi khác.",
                        "Garden Reward - Easy"));

        rewardVoucherByCycle.put(
                PlantCycleType.MEDIUM,
                upsertRewardVoucherTemplate(
                        existingByName,
                        "Giảm 30.000đ The Coffee House",
                        "The Coffee House",
                        new BigDecimal("50"),
                        600,
                        validUntil,
                        "Voucher giảm 30.000đ cho hóa đơn từ 120.000đ tại The Coffee House cho cây chu kỳ trung bình (50-80 ngày).",
                        "Mỗi tài khoản dùng 1 lần/tháng, áp dụng tại cửa hàng tham gia chương trình.",
                        "Garden Reward - Medium"));

        rewardVoucherByCycle.put(
                PlantCycleType.HARD,
                upsertRewardVoucherTemplate(
                        existingByName,
                        "Giảm 15% Jollibee",
                        "Jollibee Việt Nam",
                        new BigDecimal("80"),
                        400,
                        validUntil,
                        "Voucher giảm 15% tối đa 45.000đ tại Jollibee cho cây chu kỳ khó (90-150 ngày).",
                        "Áp dụng cho hóa đơn từ 120.000đ, không áp dụng cùng ưu đãi combo khác.",
                        "Garden Reward - Hard"));

        log.info("Seeded/updated {} garden reward voucher templates", rewardVoucherByCycle.size());
        return rewardVoucherByCycle;
    }

    private VoucherTemplateEntity upsertRewardVoucherTemplate(
            Map<String, VoucherTemplateEntity> existingByName,
            String name,
            String partnerName,
            BigDecimal requiredPoints,
            int totalStock,
            LocalDateTime validUntil,
                        String description,
                        String usageConditions,
                        String... legacyNames) {

        VoucherTemplateEntity template = existingByName.get(name);
                if (template == null && legacyNames != null) {
                        for (String legacyName : legacyNames) {
                                VoucherTemplateEntity legacyTemplate = existingByName.get(legacyName);
                                if (legacyTemplate != null) {
                                        template = legacyTemplate;
                                        break;
                                }
                        }
                }

        if (template == null) {
            template = VoucherTemplateEntity.builder()
                    .name(name)
                    .build();
        }

                template.setName(name);
        template.setPartnerName(partnerName);
        template.setDescription(description);
        template.setRequiredPoints(requiredPoints);
        template.setTotalStock(totalStock);
        template.setRemainingStock(totalStock);
                template.setUsageConditions(usageConditions);
        template.setValidUntil(validUntil);
        template.setStatus(VoucherTemplateStatus.ACTIVE);

        VoucherTemplateEntity saved = voucherTemplateRepository.save(template);
        existingByName.put(name, saved);
                if (legacyNames != null) {
                        for (String legacyName : legacyNames) {
                                if (!name.equals(legacyName)) {
                                        existingByName.remove(legacyName);
                                }
                        }
                }
        return saved;
    }

    private void seedGardenSeeds(Map<PlantCycleType, VoucherTemplateEntity> rewardVoucherByCycle) {
        List<SeedEntity> existingSeeds = seedRepository.findAll();
        Map<String, SeedEntity> existingByName = new HashMap<>();

        for (SeedEntity seed : existingSeeds) {
            existingByName.put(seed.getName(), seed);
        }

        Set<String> targetSeedNames = Set.of(
                "Hướng dương",
                "Hoa hồng",
                "Cẩm chướng",
                "Sen",
                "Anh đào",
                "Cây phong",
                "Cây thông",
                "Cây dừa",
                "Hoa tulip",
                "Hoa mai",
                "Hoa lan",
                "Cây táo",
                "Tre",
                "Xương rồng nở hoa");

        List<SeedEntity> seedsToSave = new ArrayList<>();
        int deactivatedLegacySeeds = 0;

        for (SeedEntity existingSeed : existingSeeds) {
            if (!targetSeedNames.contains(existingSeed.getName()) && Boolean.TRUE.equals(existingSeed.getIsActive())) {
                existingSeed.setIsActive(false);
                existingSeed.setRewardVoucherTemplate(null);
                seedsToSave.add(existingSeed);
                deactivatedLegacySeeds++;
            }
        }

        seedsToSave.add(upsertSeed(existingByName, "Hướng dương", 30, 3, 8, 18,
                PlantCycleType.EASY,
                rewardVoucherByCycle.get(PlantCycleType.EASY),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/huong%20duong.png?updatedAt=1776237615262"));
        seedsToSave.add(upsertSeed(existingByName, "Hoa hồng", 40, 4, 11, 23,
                PlantCycleType.EASY,
                rewardVoucherByCycle.get(PlantCycleType.EASY),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/hoa%20hong.png?updatedAt=1776237615255"));
        seedsToSave.add(upsertSeed(existingByName, "Cẩm chướng", 45, 4, 11, 26,
                PlantCycleType.EASY,
                rewardVoucherByCycle.get(PlantCycleType.EASY),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/Cam%20chuong.png?updatedAt=1776237615259"));
        seedsToSave.add(upsertSeed(existingByName, "Hoa tulip", 30, 3, 8, 19,
                PlantCycleType.EASY,
                rewardVoucherByCycle.get(PlantCycleType.EASY),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/tulip.png?updatedAt=1776237615304"));

        seedsToSave.add(upsertSeed(existingByName, "Sen", 50, 4, 11, 31,
                PlantCycleType.MEDIUM,
                rewardVoucherByCycle.get(PlantCycleType.MEDIUM),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/sen.png?updatedAt=1776237615287"));
        seedsToSave.add(upsertSeed(existingByName, "Hoa mai", 60, 4, 11, 36,
                PlantCycleType.MEDIUM,
                rewardVoucherByCycle.get(PlantCycleType.MEDIUM),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/hoa%20mai.png?updatedAt=1776237615240"));
        seedsToSave.add(upsertSeed(existingByName, "Hoa lan", 70, 5, 13, 41,
                PlantCycleType.MEDIUM,
                rewardVoucherByCycle.get(PlantCycleType.MEDIUM),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/phong%20lan.png?updatedAt=1776237615251"));
        seedsToSave.add(upsertSeed(existingByName, "Tre", 80, 5, 13, 41,
                PlantCycleType.MEDIUM,
                rewardVoucherByCycle.get(PlantCycleType.MEDIUM),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/tree.png?updatedAt=1776237615294"));
        seedsToSave.add(upsertSeed(existingByName, "Xương rồng nở hoa", 60, 4, 11, 31,
                PlantCycleType.MEDIUM,
                rewardVoucherByCycle.get(PlantCycleType.MEDIUM),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/xuong%20rong.png?updatedAt=1776237615280"));

        seedsToSave.add(upsertSeed(existingByName, "Anh đào", 90, 6, 16, 46,
                PlantCycleType.HARD,
                rewardVoucherByCycle.get(PlantCycleType.HARD),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/anh%20dao.png?updatedAt=1776237615253"));
        seedsToSave.add(upsertSeed(existingByName, "Cây phong", 100, 6, 16, 51,
                PlantCycleType.HARD,
                rewardVoucherByCycle.get(PlantCycleType.HARD),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/Cay%20phong.png?updatedAt=1776237615249"));
        seedsToSave.add(upsertSeed(existingByName, "Cây thông", 120, 8, 22, 61,
                PlantCycleType.HARD,
                rewardVoucherByCycle.get(PlantCycleType.HARD),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/Cay%20thong.png?updatedAt=1776237615270"));
        seedsToSave.add(upsertSeed(existingByName, "Cây táo", 100, 6, 16, 51,
                PlantCycleType.HARD,
                rewardVoucherByCycle.get(PlantCycleType.HARD),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/cay%20tao.png?updatedAt=1776237615273"));
        seedsToSave.add(upsertSeed(existingByName, "Cây dừa", 150, 8, 22, 71,
                PlantCycleType.HARD,
                rewardVoucherByCycle.get(PlantCycleType.HARD),
                "https://ik.imagekit.io/ii5tr5cdi/Material/Image/Garden/Cay%20dua.png?updatedAt=1776237615246"));

        seedRepository.saveAll(seedsToSave);

        log.info(
                "Seeded/updated {} garden seeds and deactivated {} legacy seeds",
                targetSeedNames.size(),
                deactivatedLegacySeeds);
    }

    private SeedEntity upsertSeed(
            Map<String, SeedEntity> existingByName,
            String name,
            int daysToMature,
            int stage2FromDay,
            int stage3FromDay,
            int stage4FromDay,
            PlantCycleType cycleType,
            VoucherTemplateEntity rewardVoucherTemplate,
            String stage4ImageUrl) {

        SeedEntity seed = existingByName.get(name);
        if (seed == null) {
            seed = SeedEntity.builder().name(name).build();
            existingByName.put(name, seed);
        }

        seed.setName(name);
        seed.setStage1ImageUrl(STAGE_SEED_IMAGE_URL);
        seed.setStage2ImageUrl(STAGE_SPROUT_IMAGE_URL);
        seed.setStage3ImageUrl(STAGE_GROWING_IMAGE_URL);
        seed.setStage4ImageUrl(stage4ImageUrl);
        seed.setDaysToMature(daysToMature);
        seed.setStage2FromDay(stage2FromDay);
        seed.setStage3FromDay(stage3FromDay);
        seed.setStage4FromDay(stage4FromDay);
        seed.setCycleType(cycleType);
        seed.setRewardVoucherTemplate(rewardVoucherTemplate);
        seed.setIsActive(true);

        return seed;
    }
}
