package com.webdev.greenify.trashspot.scheduler;

import com.webdev.greenify.trashspot.service.TrashSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotScoreScheduler {

    private final TrashSpotService trashSpotService;

    @Scheduled(fixedDelay = 3_600_000)
    public void recalculateAllActiveHotScores() {
        log.info("Starting scheduled trash spot hot score recalculation");
        try {
            int updatedCount = trashSpotService.recalculateAllActiveHotScores();
            log.info("Completed scheduled trash spot hot score recalculation. Updated spots={}", updatedCount);
        } catch (Exception ex) {
            log.error("Error during scheduled trash spot hot score recalculation", ex);
        }
    }
}
