package com.webdev.greenify.leaderboard.scheduler;

import com.webdev.greenify.leaderboard.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardScheduler {

    private final LeaderboardService leaderboardService;

    @Scheduled(fixedDelay = 60_000)
    public void checkAndFinalizeWeeks() {
        try {
            leaderboardService.finalizeDueWeeks();
        } catch (Exception ex) {
            log.error("Error while checking leaderboard weeks to finalize", ex);
        }
    }
}
