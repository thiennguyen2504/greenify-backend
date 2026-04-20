package com.webdev.greenify.user.specification;

import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.enumeration.RoleName;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<UserEntity> hasRole(RoleName role) {
        return (root, query, cb) -> {
            if (role == null) {
                return cb.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            String normalizedRole = role.name().toLowerCase(Locale.ROOT);
            return cb.equal(cb.lower(root.join("roles", JoinType.LEFT).get("name")), normalizedRole);
        };
    }

    public static Specification<UserEntity> searchByNameOrEmail(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            Join<Object, Object> profile = root.join("userProfile", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("username")), searchPattern),
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(profile.get("displayName")), searchPattern),
                    cb.like(cb.lower(profile.get("firstName")), searchPattern),
                    cb.like(cb.lower(profile.get("lastName")), searchPattern));
        };
    }

    public static Specification<UserEntity> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<UserEntity> hasStatus(AccountStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<UserEntity> buildSpecification(RoleName role, AccountStatus status, String search) {
        return Specification.where(isNotDeleted())
                .and(hasRole(role))
                .and(hasStatus(status))
                .and(searchByNameOrEmail(search));
    }
}