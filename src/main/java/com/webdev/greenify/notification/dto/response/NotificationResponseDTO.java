package com.webdev.greenify.notification.dto.response;

import com.webdev.greenify.notification.enumeration.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private String id;
    private String title;
    private String content;
    private NotificationType type;
    private boolean isRead;
    private String targetId;
    private LocalDateTime createdAt;
}
