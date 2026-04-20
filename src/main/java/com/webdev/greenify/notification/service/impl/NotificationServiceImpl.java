package com.webdev.greenify.notification.service.impl;

import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.notification.dto.response.NotificationResponseDTO;
import com.webdev.greenify.notification.entity.NotificationEntity;
import com.webdev.greenify.notification.mapper.NotificationMapper;
import com.webdev.greenify.notification.repository.NotificationRepository;
import com.webdev.greenify.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponseDTO> getMyNotifications(int page, int size) {
        String userId = getCurrentUserId();
        return getNotificationsByUserId(userId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponseDTO> getNotificationsByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationEntity> notificationPage = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        List<NotificationResponseDTO> content = notificationPage.getContent().stream()
                .map(notificationMapper::toResponse)
                .toList();

        return PagedResponse.of(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages());
    }

    @Override
    @Transactional
    public void markAsRead(String id) {
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        
        if (!notification.getUser().getId().equals(getCurrentUserId())) {
            throw new ResourceNotFoundException("Không tìm thấy thông báo");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsReadByUserId(getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications() {
        return notificationRepository.countByUserIdAndIsReadFalse(getCurrentUserId());
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
