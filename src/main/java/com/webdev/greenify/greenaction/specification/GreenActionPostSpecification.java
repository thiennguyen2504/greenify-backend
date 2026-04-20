package com.webdev.greenify.greenaction.specification;

import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.time.LocalDate;

public class GreenActionPostSpecification {

    private GreenActionPostSpecification() {
        // Utility class - no instantiation
    }

    public static Specification<GreenActionPostEntity> hasStatus(PostStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<GreenActionPostEntity> hasStatuses(Collection<PostStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("status").in(statuses);
        };
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

    public static Specification<GreenActionPostEntity> hasAuthorEmailOrDisplayNameLike(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String keyword = "%" + search.trim().toLowerCase() + "%";
            Join<Object, Object> userJoin = root.join("user", JoinType.INNER);
            Join<Object, Object> userProfileJoin = userJoin.join("userProfile", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(userJoin.get("email")), keyword),
                    cb.like(cb.lower(userProfileJoin.get("displayName")), keyword));
        };
    }

    public static Specification<GreenActionPostEntity> hasUserId(String userId) {
        return (root, query, cb) -> {
            if (userId == null || userId.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("user").get("id"), userId);
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
