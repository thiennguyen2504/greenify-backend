package com.webdev.greenify.config;

import com.webdev.greenify.greenaction.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to expire points after 2 months.
 * Runs daily at midnight to check for expired points and create deduction transactions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointExpirationScheduler {

    private final PointService pointService;

    /**
     * Run point expiration check daily at 00:00 AM.
     * Processes all expired points and creates deduction transactions.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void expirePoints() {
        log.info("Starting scheduled point expiration check");
        try {
            pointService.processExpiredPoints();
            log.info("Completed scheduled point expiration check");
        } catch (Exception e) {
            log.error("Error during point expiration check", e);
        }
    }
}
