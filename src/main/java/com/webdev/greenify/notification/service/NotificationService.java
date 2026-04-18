package com.webdev.greenify.notification.service;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.notification.dto.response.NotificationResponseDTO;

public interface NotificationService {
    PagedResponse<NotificationResponseDTO> getMyNotifications(int page, int size);
    PagedResponse<NotificationResponseDTO> getNotificationsByUserId(String userId, int page, int size);
    void markAsRead(String id);
    void markAllAsRead();
    long countUnreadNotifications();
}
