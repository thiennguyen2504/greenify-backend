package com.webdev.greenify.co2e.listener;

import com.webdev.greenify.co2e.event.Co2eAnalysisEvent;
import com.webdev.greenify.co2e.service.Co2eService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Async listener to process CO2e analysis side effects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Co2eAnalysisListener {

    private final Co2eService co2eService;

    @Async
    @EventListener
    @Transactional
    public void handleCo2eAnalysis(Co2eAnalysisEvent event) {
        log.info("CO2e LISTENER INVOKED for post: {}, user: {}", event.getPostId(), event.getUserId());
        try {
            log.info("Starting CO2e analysis service for post {}", event.getPostId());
            co2eService.processPostCo2e(event);
            log.info("CO2e analysis service completed for post {}", event.getPostId());
        } catch (Exception ex) {
            log.error("CO2e analysis failed for post {}: {}", event.getPostId(), ex.getMessage(), ex);
        }
    }
}
