package com.webdev.greenify.notification.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    EVENT_APPROVED("Event Approved"),
    EVENT_REJECTED("Event Rejected"),
    POINT_RECEIVED("Points Received"),
    EVENT_CREATED_SUCCESS("Event Created Successfully"),
    OTHER("Other");

    private final String description;
}
