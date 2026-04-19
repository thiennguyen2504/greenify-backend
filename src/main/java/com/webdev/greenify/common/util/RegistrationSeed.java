package com.webdev.greenify.common.util;

import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import com.webdev.greenify.greenaction.repository.EventRegistrationRepository;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationSeed {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public void seed() {
        if (registrationRepository.count() > 10) {
            return;
        }

        RoleEntity userRole = roleRepository.findByName("USER").orElseThrow();

        // Create some normal users for registrations
        List<String> userEmails = List.of(
                "volunteer1@example.com",
                "volunteer2@example.com",
                "volunteer3@example.com",
                "volunteer4@example.com",
                "volunteer5@example.com"
        );

        List<UserEntity> participants = new ArrayList<>();
        for (String email : userEmails) {
            UserEntity user = userRepository.findByIdentifier(email).orElseGet(() -> {
                Set<RoleEntity> roles = new HashSet<>();
                roles.add(userRole);
                UserEntity newUser = UserEntity.builder()
                        .email(email)
                        .username(email.split("@")[0])
                        .password(passwordEncoder.encode("password123"))
                        .roles(roles)
                        .status(AccountStatus.ACTIVE)
                        .build();
                return userRepository.save(newUser);
            });
            participants.add(user);
        }

        List<EventEntity> events = eventRepository.findAll();
        if (events.isEmpty() || participants.isEmpty()) {
            log.warn("Not enough events or participants to seed registrations.");
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            EventEntity event = events.get(i);
            // Each event gets 1-3 registrations
            int count = (i % 3) + 1;
            for (int j = 0; j < count; j++) {
                UserEntity participant = participants.get((i + j) % participants.size());

                if (registrationRepository.findByEventIdAndUserId(event.getId(), participant.getId()).isEmpty()) {
                    EventRegistrationEntity registration = EventRegistrationEntity.builder()
                            .event(event)
                            .user(participant)
                            .status(RegistrationStatus.REGISTERED)
                            .registrationCode("REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .build();

                    registrationRepository.save(registration);

                    // Update event participant count
                    Long currentCount = event.getParticipantCount() != null ? event.getParticipantCount() : 0L;
                    event.setParticipantCount(currentCount + 1);
                    eventRepository.save(event);
                }
            }
        }
        log.info("Seeded registrations for events.");
    }
}
