package com.webdev.greenify.common.util;

import com.webdev.greenify.streak.entity.StreakEntity;
import com.webdev.greenify.streak.enumeration.StreakStatus;
import com.webdev.greenify.streak.repository.StreakRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreakSeed {

    private static final long SEED_THRESHOLD = 5;

    private final StreakRepository streakRepository;
    private final UserRepository userRepository;

    @Transactional
    public void seed() {
        if (streakRepository.count() > SEED_THRESHOLD) {
            log.info("Skip StreakSeed because streak count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDate currentMonth = today.withDayOfMonth(1);

            List<StreakSeedItem> seedItems = List.of(
                    new StreakSeedItem("user1", 12, 15, StreakStatus.ACTIVE, today.minusDays(1), null, 0, 0),
                    new StreakSeedItem("user2", 7, 7, StreakStatus.ACTIVE, today.minusDays(1), null, 0, 0),
                    new StreakSeedItem("ctv1", 0, 20, StreakStatus.BROKEN, today.minusDays(2), today.minusDays(1), 6, 0),
                    new StreakSeedItem("user3", 3, 10, StreakStatus.ACTIVE, today, null, 0, 0),
                    new StreakSeedItem("user4", 1, 5, StreakStatus.ACTIVE, today.minusDays(1), null, 0, 0),
                    new StreakSeedItem("user5", 5, 8, StreakStatus.ACTIVE, today.minusDays(1), null, 0, 0),
                    new StreakSeedItem("ctv2", 15, 25, StreakStatus.ACTIVE, today.minusDays(1), null, 0, 0),
                    new StreakSeedItem("user6", 0, 3, StreakStatus.BROKEN, today.minusDays(5), today.minusDays(4), 3, 3),
                    new StreakSeedItem("user7", 2, 2, StreakStatus.ACTIVE, today.minusDays(1), null, 0, 0),
                    new StreakSeedItem("ctv3", 8, 12, StreakStatus.ACTIVE, today.minusDays(1), null, 0, 0)
            );

            for (StreakSeedItem seedItem : seedItems) {
                try {
                    UserEntity user = findUserByUsername(seedItem.username());
                    if (user == null) {
                        log.warn("Skip streak record because user {} not found", seedItem.username());
                        continue;
                    }

                    StreakEntity streak = streakRepository.findByUserId(user.getId())
                            .orElseGet(() -> StreakEntity.builder().user(user).build());

                    streak.setUser(user);
                    streak.setCurrentStreak(seedItem.currentStreak());
                    streak.setLongestStreak(seedItem.longestStreak());
                    streak.setStatus(seedItem.status());
                    streak.setLastValidDate(seedItem.lastValidDate());
                    streak.setRestoreMonth(currentMonth);

                    if (seedItem.status() == StreakStatus.BROKEN) {
                        streak.setLastBreakDate(seedItem.lastBreakDate());
                        streak.setBrokenStreak(seedItem.brokenStreak());
                        streak.setRestoreUsedThisMonth(seedItem.restoreUsedThisMonth());
                    } else {
                        streak.setLastBreakDate(null);
                        streak.setBrokenStreak(0);
                        streak.setRestoreUsedThisMonth(0);
                    }

                    streakRepository.save(streak);
                    log.info("Seeded streak for {}", seedItem.username());
                } catch (Exception ex) {
                    log.warn("Skip streak record {} due to error: {}", seedItem.username(), ex.getMessage());
                }
            }

            log.info("StreakSeed completed");
        } catch (Exception e) {
            log.warn("StreakSeed failed: {}", e.getMessage(), e);
        }
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .or(() -> userRepository.findByIdentifier(username + "@greenify.vn"))
                .orElse(null);
    }

    private record StreakSeedItem(
            String username,
            int currentStreak,
            int longestStreak,
            StreakStatus status,
            LocalDate lastValidDate,
            LocalDate lastBreakDate,
            int brokenStreak,
            int restoreUsedThisMonth) {
    }
}
