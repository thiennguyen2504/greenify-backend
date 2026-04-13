package com.webdev.greenify.streak.service;

import com.webdev.greenify.streak.dto.response.StreakResponse;

import java.time.LocalDate;

public interface StreakService {

    void handleVerifiedPost(String userId, LocalDate actionDate);

    StreakResponse getCurrentStreak();

    StreakResponse restoreStreak();
}
