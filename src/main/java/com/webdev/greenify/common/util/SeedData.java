package com.webdev.greenify.common.util;

import com.webdev.greenify.garden.entity.SeedEntity;
import com.webdev.greenify.garden.repository.SeedRepository;
import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.station.repository.WasteTypeRepository;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class SeedData implements CommandLineRunner {

        private static final String SEEDED_PASSWORD = "password123";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GreenActionTypeRepository greenActionTypeRepository;
    private final WasteTypeRepository wasteTypeRepository;
        private final SeedRepository seedRepository;
        private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

                // Backfill legacy null soft-delete flags before any entity mapping occurs.
                normalizeSoftDeleteFlags();

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

                // Seed Garden Seeds
                seedGardenSeeds();
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
                    .groupName("Waste Sorting")
                    .actionName("Sorting waste at home")
                    .suggestedPoints(new BigDecimal("5"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Waste Sorting")
                    .actionName("Sorting waste at work/school")
                    .suggestedPoints(new BigDecimal("6"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Recycling")
                    .actionName("Collect paper/plastic/cans for recycling")
                    .suggestedPoints(new BigDecimal("5"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Recycling")
                    .actionName("Bring recyclables to collection points")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Plastic Reduction")
                    .actionName("Use personal water bottles")
                    .suggestedPoints(new BigDecimal("2"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Plastic Reduction")
                    .actionName("Use personal containers/cups when buying food")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Plastic Reduction")
                    .actionName("Refuse plastic bags/straws")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Resource Conservation")
                    .actionName("Turn off electricity when not in use")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Resource Conservation")
                    .actionName("Conserve water in daily activities")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Green Transportation")
                    .actionName("Walk/Bike for short distances")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Green Transportation")
                    .actionName("Use public transportation")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Environmental Cleanup")
                    .actionName("Pick up trash in public areas")
                    .suggestedPoints(new BigDecimal("8"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Environmental Cleanup")
                    .actionName("Clean up local living neighborhood")
                    .suggestedPoints(new BigDecimal("6"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Greenery")
                    .actionName("Plant trees/flowers")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Greenery")
                    .actionName("Regularly care for plants")
                    .suggestedPoints(new BigDecimal("2"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Reuse")
                    .actionName("Reuse items instead of throwing them away")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Green Consumption")
                    .actionName("Buy eco-friendly products")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Community Participation")
                    .actionName("Join environmental events organized by App/NGO")
                    .suggestedPoints(new BigDecimal("10"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Green Awareness")
                    .actionName("Share environmental messages with practical actions")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Green Initiative")
                    .actionName("Organize or initiate small green activities")
                    .suggestedPoints(new BigDecimal("9"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Environmental Reporting")
                    .actionName("Report illegal dumping/polluted spots")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Creative Recycling")
                    .actionName("DIY products from recycled materials")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Organic at Home")
                    .actionName("Compost organic wastes at home")
                    .suggestedPoints(new BigDecimal("8"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Community Contribution")
                    .actionName("Review posts as a Contributor")
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
                    .name("Plastic")
                    .description("Plastic bottles, containers, bags, and household plastic items.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Paper")
                    .description("Newspapers, books, cardboard, and office paper.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Metal")
                    .description("Aluminum cans, scrap iron, aluminum, and copper.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Glass")
                    .description("Glass bottles, jars, and broken glassware.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Organic")
                    .description("Food scraps, fruit peels, vegetables, and garden waste.")
                    .build());

            wasteTypes.add(WasteTypeEntity.builder()
                    .name("Hazardous")
                    .description("Old batteries, fluorescent bulbs, and chemical containers.")
                    .build());

            wasteTypeRepository.saveAll(wasteTypes);
            log.info("Seeded {} waste types", wasteTypes.size());
        }
    }

    private void seedGardenSeeds() {
        if (seedRepository.count() == 0) {
            List<SeedEntity> seeds = new ArrayList<>();

            seeds.add(SeedEntity.builder()
                    .name("Sunflower")
                    .stage1ImageUrl("https://picsum.photos/seed/greenify-sunflower-stage1/600/600")
                    .stage2ImageUrl("https://picsum.photos/seed/greenify-sunflower-stage2/600/600")
                    .stage3ImageUrl("https://picsum.photos/seed/greenify-sunflower-stage3/600/600")
                    .stage4ImageUrl("https://picsum.photos/seed/greenify-sunflower-stage4/600/600")
                    .daysToMature(14)
                    .stage2FromDay(3)
                    .stage3FromDay(7)
                    .stage4FromDay(12)
                    .cycleType(PlantCycleType.SHORT_TERM)
                    .isActive(true)
                    .build());

            seeds.add(SeedEntity.builder()
                    .name("Lavender")
                    .stage1ImageUrl("https://picsum.photos/seed/greenify-lavender-stage1/600/600")
                    .stage2ImageUrl("https://picsum.photos/seed/greenify-lavender-stage2/600/600")
                    .stage3ImageUrl("https://picsum.photos/seed/greenify-lavender-stage3/600/600")
                    .stage4ImageUrl("https://picsum.photos/seed/greenify-lavender-stage4/600/600")
                    .daysToMature(21)
                    .stage2FromDay(5)
                    .stage3FromDay(10)
                    .stage4FromDay(16)
                    .cycleType(PlantCycleType.LONG_TERM)
                    .isActive(true)
                    .build());

            seeds.add(SeedEntity.builder()
                    .name("Rose")
                    .stage1ImageUrl("https://picsum.photos/seed/greenify-rose-stage1/600/600")
                    .stage2ImageUrl("https://picsum.photos/seed/greenify-rose-stage2/600/600")
                    .stage3ImageUrl("https://picsum.photos/seed/greenify-rose-stage3/600/600")
                    .stage4ImageUrl("https://picsum.photos/seed/greenify-rose-stage4/600/600")
                    .daysToMature(30)
                    .stage2FromDay(6)
                    .stage3FromDay(14)
                    .stage4FromDay(24)
                    .cycleType(PlantCycleType.LONG_TERM)
                    .isActive(true)
                    .build());

            seedRepository.saveAll(seeds);
            log.info("Seeded {} garden seeds", seeds.size());
        }
    }
}
