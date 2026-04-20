package com.webdev.greenify.streak.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.garden.service.GardenService;
import com.webdev.greenify.streak.dto.response.StreakResponse;
import com.webdev.greenify.streak.entity.StreakEntity;
import com.webdev.greenify.streak.enumeration.StreakStatus;
import com.webdev.greenify.streak.mapper.StreakMapper;
import com.webdev.greenify.streak.repository.StreakRepository;
import com.webdev.greenify.streak.service.StreakService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakServiceImpl implements StreakService {

    private static final int MAX_RESTORE_PER_MONTH = 3;
    private static final int RESTORE_WINDOW_HOURS = 24;

    private final StreakRepository streakRepository;
    private final UserRepository userRepository;
    private final StreakMapper streakMapper;
    private final GardenService gardenService;

    /**
     * Keep streak and review status in a single transaction for strong consistency.
     * If streak update fails, review submission is rolled back to avoid data divergence.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleVerifiedPost(String userId, LocalDate actionDate, String greenPostUrl) {
        if (userId == null || actionDate == null) {
            return;
        }

        StreakEntity streak = getOrCreateStreak(userId);

        if (isDuplicateOrOutOfOrderAction(streak, actionDate)) {
            return;
        }

        if (streak.getLastValidDate() == null) {
            streak.setCurrentStreak(1);
            streak.setLongestStreak(Math.max(safeInt(streak.getLongestStreak()), 1));
            streak.setLastValidDate(actionDate);
            streak.setStatus(StreakStatus.ACTIVE);
            streakRepository.save(streak);

            gardenService.updatePlantProgress(userId, actionDate, greenPostUrl);

            log.info("Initialized streak for user {} at actionDate={}", userId, actionDate);
            return;
        }

        long dayGap = ChronoUnit.DAYS.between(streak.getLastValidDate(), actionDate);
        if (dayGap > 1) {
            // MVP break detection is lazy and handled inline on verified posts with date gaps.
            markAsBroken(streak, LocalDate.now());
            streak.setCurrentStreak(1);
            streak.setStatus(StreakStatus.ACTIVE);
        } else if (dayGap == 1) {
            streak.setCurrentStreak(safeInt(streak.getCurrentStreak()) + 1);
            streak.setStatus(StreakStatus.ACTIVE);
        }

        streak.setLastValidDate(actionDate);

        if (safeInt(streak.getCurrentStreak()) > safeInt(streak.getLongestStreak())) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streakRepository.save(streak);
        gardenService.updatePlantProgress(userId, actionDate, greenPostUrl);

        log.info("Handled verified post for user {} on {}. currentStreak={}, longestStreak={}",
                userId,
                actionDate,
                streak.getCurrentStreak(),
                streak.getLongestStreak());
    }

    @Override
    @Transactional
    public StreakResponse getCurrentStreak() {
        String userId = getCurrentUserId();
        StreakEntity streak = getOrCreateStreak(userId);

        applyLazyBreakDetection(streak);
        streak = streakRepository.save(streak);

        return toStreakResponse(streak);
    }

    @Override
    @Transactional
    public StreakResponse restoreStreak() {
        String userId = getCurrentUserId();
        StreakEntity streak = getOrCreateStreak(userId);

        applyLazyBreakDetection(streak);

        if (streak.getStatus() != StreakStatus.BROKEN) {
            throw new AppException("Chuỗi streak chưa bị ngắt", HttpStatus.BAD_REQUEST);
        }

        resetRestoreQuotaIfMonthChanged(streak);

        if (safeInt(streak.getRestoreUsedThisMonth()) >= MAX_RESTORE_PER_MONTH) {
            throw new AppException("Đã vượt quá số lần khôi phục trong tháng", HttpStatus.BAD_REQUEST);
        }

        if (!isWithinRestoreWindow(streak.getLastBreakDate())) {
            throw new AppException("Đã hết thời gian cho phép khôi phục", HttpStatus.BAD_REQUEST);
        }

        streak.setRestoreUsedThisMonth(safeInt(streak.getRestoreUsedThisMonth()) + 1);
        streak.setCurrentStreak(safeInt(streak.getBrokenStreak()));
        streak.setStatus(StreakStatus.ACTIVE);
        streak.setLastValidDate(streak.getLastBreakDate());

        streak = streakRepository.save(streak);

        log.info("Restored streak for user {} with currentStreak={} and restoreUsedThisMonth={}",
                userId,
                streak.getCurrentStreak(),
                streak.getRestoreUsedThisMonth());

        return toStreakResponse(streak);
    }

    private StreakResponse toStreakResponse(StreakEntity streak) {
        StreakResponse response = streakMapper.toStreakResponse(streak);

        int restoreUsedInCurrentMonth = resolveCurrentMonthRestoreUsed(streak);
        response.setRestoreUsedThisMonth(restoreUsedInCurrentMonth);
        response.setRestoreAvailable(isRestoreAvailable(streak, restoreUsedInCurrentMonth));

        return response;
    }

    private boolean isDuplicateOrOutOfOrderAction(StreakEntity streak, LocalDate actionDate) {
        if (streak.getLastValidDate() == null) {
            return false;
        }

        if (actionDate.isEqual(streak.getLastValidDate())) {
            log.info("Skip streak update for user {} on {} due to idempotent same-day verification",
                    streak.getUser().getId(),
                    actionDate);
            return true;
        }

        if (actionDate.isBefore(streak.getLastValidDate())) {
            log.info("Skip streak update for user {} because actionDate {} is before lastValidDate {}",
                    streak.getUser().getId(),
                    actionDate,
                    streak.getLastValidDate());
            return true;
        }

        return false;
    }

    private void applyLazyBreakDetection(StreakEntity streak) {
        if (streak.getStatus() != StreakStatus.ACTIVE || streak.getLastValidDate() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (today.isAfter(streak.getLastValidDate().plusDays(1))) {
            markAsBroken(streak, today);
        }
    }

    private void markAsBroken(StreakEntity streak, LocalDate breakDetectedDate) {
        if (streak.getStatus() == StreakStatus.BROKEN) {
            return;
        }

        int previousCurrent = safeInt(streak.getCurrentStreak());

        streak.setStatus(StreakStatus.BROKEN);
        streak.setLastBreakDate(breakDetectedDate);
        streak.setBrokenStreak(previousCurrent);
        streak.setCurrentStreak(0);

        log.info("Streak broken for user {} at {}. brokenStreak={}",
                streak.getUser().getId(),
                breakDetectedDate,
                previousCurrent);
    }

    private StreakEntity getOrCreateStreak(String userId) {
        return streakRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

                    StreakEntity newStreak = StreakEntity.builder()
                            .user(user)
                            .currentStreak(0)
                            .longestStreak(0)
                            .status(StreakStatus.NOT_STARTED)
                            .restoreUsedThisMonth(0)
                            .restoreMonth(firstDayOfCurrentMonth())
                            .brokenStreak(0)
                            .build();

                    return streakRepository.save(newStreak);
                });
    }

    private void resetRestoreQuotaIfMonthChanged(StreakEntity streak) {
        LocalDate currentMonth = firstDayOfCurrentMonth();
        if (streak.getRestoreMonth() == null || !streak.getRestoreMonth().isEqual(currentMonth)) {
            streak.setRestoreMonth(currentMonth);
            streak.setRestoreUsedThisMonth(0);
        }
    }

    private int resolveCurrentMonthRestoreUsed(StreakEntity streak) {
        if (streak.getRestoreMonth() == null || !streak.getRestoreMonth().isEqual(firstDayOfCurrentMonth())) {
            return 0;
        }
        return safeInt(streak.getRestoreUsedThisMonth());
    }

    private boolean isRestoreAvailable(StreakEntity streak, int restoreUsedInCurrentMonth) {
        return streak.getStatus() == StreakStatus.BROKEN
                && restoreUsedInCurrentMonth < MAX_RESTORE_PER_MONTH
                && isWithinRestoreWindow(streak.getLastBreakDate());
    }

    private boolean isWithinRestoreWindow(LocalDate lastBreakDate) {
        if (lastBreakDate == null) {
            return false;
        }

        // Calendar-day interpretation of a 24-hour restore policy.
        long restoreWindowDays = RESTORE_WINDOW_HOURS / 24L;
        return !LocalDate.now().isAfter(lastBreakDate.plusDays(restoreWindowDays));
    }

    private LocalDate firstDayOfCurrentMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
