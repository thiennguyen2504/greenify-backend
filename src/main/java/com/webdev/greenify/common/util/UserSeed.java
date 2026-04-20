package com.webdev.greenify.common.util;

import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.UserProfileEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.enumeration.UserProfileStatus;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserProfileRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeed {

    private static final long SEED_THRESHOLD = 10;
    private static final String DEFAULT_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void seed() {
        if (userRepository.count() > SEED_THRESHOLD) {
            log.info("Skip UserSeed because user count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            RoleEntity userRole = roleRepository.findByName("USER").orElse(null);
            RoleEntity ctvRole = roleRepository.findByName("CTV").orElse(null);
            if (userRole == null || ctvRole == null) {
                log.warn("Skip UserSeed because required roles USER/CTV are missing");
                return;
            }

            String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

            for (SeedUser seedUser : buildSeedUsers()) {
                try {
                    upsertUser(seedUser, userRole, ctvRole, encodedPassword);
                } catch (Exception ex) {
                    log.warn("Skip seeding user {} due to error: {}", seedUser.username(), ex.getMessage());
                }
            }

            log.info("UserSeed completed");
        } catch (Exception e) {
            log.warn("UserSeed failed: {}", e.getMessage(), e);
        }
    }

    private void upsertUser(SeedUser seedUser, RoleEntity userRole, RoleEntity ctvRole, String encodedPassword) {
        UserEntity user = findUserByUsername(seedUser.username());
        if (user == null) {
            user = UserEntity.builder().build();
        }

        user.setUsername(seedUser.username());
        user.setEmail(seedUser.email());
        user.setPassword(encodedPassword);
        user.setStatus(AccountStatus.ACTIVE);

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);
        if (seedUser.collaborator()) {
            roles.add(ctvRole);
        }
        user.setRoles(roles);

        user = userRepository.save(user);

        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId()).orElseGet(UserProfileEntity::new);
        profile.setDisplayName(seedUser.fullName());
        profile.setProvince(seedUser.province());
        profile.setStatus(UserProfileStatus.COMPLETE);
        profile.setUser(user);

        // Keep both sides in sync for the bidirectional relation.
        user.setUserProfile(profile);

        userProfileRepository.save(profile);

        log.info("Seeded user {} ({})", seedUser.username(), seedUser.fullName());
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .or(() -> userRepository.findByIdentifier(username + "@greenify.vn"))
                .orElse(null);
    }

    private List<SeedUser> buildSeedUsers() {
        return List.of(
                new SeedUser("user1", "user1@greenify.vn", "Nguyễn Văn An", "Thành phố Hồ Chí Minh", false),
                new SeedUser("user2", "user2@greenify.vn", "Trần Thị Bình", "Hà Nội", false),
                new SeedUser("user3", "user3@greenify.vn", "Phạm Thu Hà", "Đà Nẵng", false),
                new SeedUser("user4", "user4@greenify.vn", "Hoàng Đức Long", "Cần Thơ", false),
                new SeedUser("user5", "user5@greenify.vn", "Vũ Thanh Tùng", "Hải Phòng", false),
                new SeedUser("user6", "user6@greenify.vn", "Đặng Thị Mai", "Thành phố Hồ Chí Minh", false),
                new SeedUser("user7", "user7@greenify.vn", "Bùi Quang Huy", "Hà Nội", false),
                new SeedUser("ctv1", "ctv1@greenify.vn", "Lê Minh Cường", "Thành phố Hồ Chí Minh", true),
                new SeedUser("ctv2", "ctv2@greenify.vn", "Ngô Thị Lan", "Hà Nội", true),
                new SeedUser("ctv3", "ctv3@greenify.vn", "Đinh Văn Khải", "Đà Nẵng", true)
        );
    }

    private record SeedUser(String username, String email, String fullName, String province, boolean collaborator) {
    }
}
