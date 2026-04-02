package com.webdev.greenify.greenaction.specification;

import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class GreenActionPostSpecification {

    private GreenActionPostSpecification() {
        // Utility class - no instantiation
    }

    public static Specification<GreenActionPostEntity> hasStatus(PostStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<GreenActionPostEntity> hasActionTypeId(String actionTypeId) {
        return (root, query, cb) -> {
            if (actionTypeId == null || actionTypeId.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("actionType").get("id"), actionTypeId);
        };
    }

    public static Specification<GreenActionPostEntity> hasGroupNameLike(String groupName) {
        return (root, query, cb) -> {
            if (groupName == null || groupName.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("actionType").get("groupName")), 
                    "%" + groupName.toLowerCase() + "%");
        };
    }

    public static Specification<GreenActionPostEntity> actionDateFrom(LocalDate fromDate) {
        return (root, query, cb) -> {
            if (fromDate == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("actionDate"), fromDate);
        };
    }

    public static Specification<GreenActionPostEntity> actionDateTo(LocalDate toDate) {
        return (root, query, cb) -> {
            if (toDate == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("actionDate"), toDate);
        };
    }

    public static Specification<GreenActionPostEntity> buildSpecification(
            PostStatus status,
            String actionTypeId,
            String groupName,
            LocalDate fromDate,
            LocalDate toDate) {
        
        return hasStatus(status)
                .and(hasActionTypeId(actionTypeId))
                .and(hasGroupNameLike(groupName))
                .and(actionDateFrom(fromDate))
                .and(actionDateTo(toDate));
    }
}
