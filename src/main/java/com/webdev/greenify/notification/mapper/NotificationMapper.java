package com.webdev.greenify.notification.mapper;

import com.webdev.greenify.notification.dto.response.NotificationResponseDTO;
import com.webdev.greenify.notification.entity.NotificationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponseDTO toResponse(NotificationEntity entity);
}
