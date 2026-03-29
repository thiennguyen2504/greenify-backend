package com.webdev.greenify.common.util;

import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SeedData implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // Ensure roleEntities exist
        RoleEntity adminRoleEntity = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ADMIN").build()));

        RoleEntity userRoleEntity = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("USER").build()));

        // Create Admin UserEntity
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            Set<RoleEntity> adminRoleEntities = new HashSet<>();
            adminRoleEntities.add(adminRoleEntity);
            adminRoleEntities.add(userRoleEntity);

            userRepository.save(UserEntity.builder()
                    .firstname("Admin")
                    .lastname("UserEntity")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .roles(adminRoleEntities)
                    .build());
        }

        // Create Normal UserEntity
        if (userRepository.findByEmail("user@example.com").isEmpty()) {
            Set<RoleEntity> userRoleEntities = new HashSet<>();
            userRoleEntities.add(userRoleEntity);

            userRepository.save(UserEntity.builder()
                    .firstname("Normal")
                    .lastname("UserEntity")
                    .email("user@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .roles(userRoleEntities)
                    .build());
        }
    }
}
