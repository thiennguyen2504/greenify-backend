package com.webdev.greenify.notification.event;

import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.notification.entity.NotificationEntity;
import com.webdev.greenify.notification.repository.NotificationRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @EventListener
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Handling notification event for user: {}", event.getUserId());
        
        UserEntity user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + event.getUserId()));

        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .title(event.getTitle())
                .content(event.getContent())
                .type(event.getType())
                .targetId(event.getTargetId())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Notification saved for user: {}", event.getUserId());
    }
}
