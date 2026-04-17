package com.webdev.greenify.garden.specification;

import com.webdev.greenify.garden.entity.PlantDailyLogEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class PlantDailyLogSpecification {

    private PlantDailyLogSpecification() {
        // Utility class - no instantiation
    }

    public static Specification<PlantDailyLogEntity> hasUserId(String userId) {
        return (root, query, cb) -> {
            if (userId == null || userId.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<PlantDailyLogEntity> logDateFrom(LocalDate fromDate) {
        return (root, query, cb) -> {
            if (fromDate == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("logDate"), fromDate);
        };
    }

    public static Specification<PlantDailyLogEntity> logDateTo(LocalDate toDate) {
        return (root, query, cb) -> {
            if (toDate == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("logDate"), toDate);
        };
    }

    public static Specification<PlantDailyLogEntity> buildSpecification(
            String userId,
            LocalDate fromDate,
            LocalDate toDate) {

        return hasUserId(userId)
                .and(logDateFrom(fromDate))
                .and(logDateTo(toDate));
    }
}
