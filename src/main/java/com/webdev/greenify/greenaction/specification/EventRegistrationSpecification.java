package com.webdev.greenify.greenaction.specification;

import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import org.springframework.data.jpa.domain.Specification;

public class EventRegistrationSpecification {

    private EventRegistrationSpecification() {}

    public static Specification<EventRegistrationEntity> hasUserId(String userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction() : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<EventRegistrationEntity> hasRegistrationStatus(RegistrationStatus registrationStatus) {
        return (root, query, cb) -> registrationStatus == null
                ? cb.conjunction()
                : cb.equal(root.get("registrationStatus"), registrationStatus);
    }

    public static Specification<EventRegistrationEntity> eventTitleContains(String title) {
        return (root, query, cb) -> (title == null || title.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("event").get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<EventRegistrationEntity> eventAddressContains(String address) {
        return (root, query, cb) -> {
            if (address == null || address.isBlank()) return cb.conjunction();
            String pattern = "%" + address.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("event").get("address").get("province")), pattern),
                    cb.like(cb.lower(root.get("event").get("address").get("district")), pattern),
                    cb.like(cb.lower(root.get("event").get("address").get("ward")), pattern),
                    cb.like(cb.lower(root.get("event").get("address").get("street")), pattern)
            );
        };
    }
    
    public static Specification<EventRegistrationEntity> hasEventId(String eventId) {
        return (root, query, cb) -> eventId == null ? cb.conjunction() : cb.equal(root.get("event").get("id"), eventId);
    }

    public static Specification<EventRegistrationEntity> buildSpecification(
            String userId,
            String title,
            RegistrationStatus registrationStatus,
            String address) {
        return Specification.where(hasUserId(userId))
                .and(hasRegistrationStatus(registrationStatus))
                .and(eventTitleContains(title))
                .and(eventAddressContains(address));
    }
}
