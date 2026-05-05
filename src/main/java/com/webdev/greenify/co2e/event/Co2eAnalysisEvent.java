package com.webdev.greenify.co2e.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class Co2eAnalysisEvent extends ApplicationEvent {

    private final String postId;
    private final String userId;
    private final String imageUrl;
    private final String caption;
    private final String actionTypeName;
    private final LocalDate actionDate;

    public Co2eAnalysisEvent(
            Object source,
            String postId,
            String userId,
            String imageUrl,
            String caption,
            String actionTypeName,
            LocalDate actionDate) {
        super(source);
        this.postId = postId;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.actionTypeName = actionTypeName;
        this.actionDate = actionDate;
    }
}
