package com.webdev.greenify.greenaction.specification;

import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class EventSpecification {

    private EventSpecification() {
        // Utility class
    }

    public static Specification<EventEntity> hasStatus(GreenEventStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<EventEntity> hasEventType(GreenEventType eventType) {
        return (root, query, cb) -> eventType == null ? cb.conjunction() : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<EventEntity> titleContains(String title) {
        return (root, query, cb) -> (title == null || title.isBlank()) 
                ? cb.conjunction() 
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<EventEntity> startTimeAfter(LocalDateTime from) {
        return (root, query, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("startTime"), from);
    }

    public static Specification<EventEntity> endTimeBefore(LocalDateTime to) {
        return (root, query, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("endTime"), to);
    }

    public static Specification<EventEntity> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("isDeleted"), false);
    }

    public static Specification<EventEntity> buildSpecification(
            GreenEventStatus status,
            GreenEventType eventType,
            String title,
            LocalDateTime from,
            LocalDateTime to) {
        return Specification.where(hasStatus(status))
                .and(hasEventType(eventType))
                .and(titleContains(title))
                .and(startTimeAfter(from))
                .and(endTimeBefore(to))
                .and(isNotDeleted());
    }
}
