package com.webdev.greenify.user.specification;

import com.webdev.greenify.user.entity.NGOProfileEntity;
import com.webdev.greenify.user.enumeration.NGOProfileStatus;
import org.springframework.data.jpa.domain.Specification;

public class NGOProfileSpecification {

    private NGOProfileSpecification() {}

    public static Specification<NGOProfileEntity> hasOrgName(String orgName) {
        return (root, query, cb) -> (orgName == null || orgName.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("orgName")), "%" + orgName.toLowerCase() + "%");
    }

    public static Specification<NGOProfileEntity> hasStatus(NGOProfileStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<NGOProfileEntity> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String searchPattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("orgName")), searchPattern),
                    cb.like(cb.lower(root.get("contactEmail")), searchPattern),
                    cb.like(cb.lower(root.get("representativeName")), searchPattern)
            );
        };
    }

    public static Specification<NGOProfileEntity> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("isDeleted"), false);
    }

    public static Specification<NGOProfileEntity> buildSpecification(String orgName, NGOProfileStatus status, String search) {
        return Specification.where(hasOrgName(orgName))
                .and(hasStatus(status))
                .and(search(search))
                .and(isNotDeleted());
    }
}
