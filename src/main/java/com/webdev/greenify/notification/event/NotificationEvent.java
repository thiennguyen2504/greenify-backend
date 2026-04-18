package com.webdev.greenify.notification.event;

import com.webdev.greenify.notification.enumeration.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {
    private final String userId;
    private final String title;
    private final String content;
    private final NotificationType type;
    private final String targetId;

    public NotificationEvent(Object source, String userId, String title, String content, NotificationType type, String targetId) {
        super(source);
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.targetId = targetId;
    }
}
