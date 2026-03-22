package com.webdev.greenify.seed;

import com.webdev.greenify.entity.Role;
import com.webdev.greenify.entity.User;
import com.webdev.greenify.repository.RoleRepository;
import com.webdev.greenify.repository.UserRepository;
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

        // Ensure roles exist
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        // Create Admin User
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            adminRoles.add(userRole);

            userRepository.save(User.builder()
                    .firstname("Admin")
                    .lastname("User")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .enabled(true)
                    .roles(adminRoles)
                    .build());
        }

        // Create Normal User
        if (userRepository.findByEmail("user@example.com").isEmpty()) {
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(userRole);

            userRepository.save(User.builder()
                    .firstname("Normal")
                    .lastname("User")
                    .email("user@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .enabled(true)
                    .roles(userRoles)
                    .build());
        }
    }
}
