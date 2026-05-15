package com.webdev.greenify.point.repository;

import com.webdev.greenify.point.entity.PointLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import com.webdev.greenify.point.enumeration.PointLedgerSourceType;

public interface PointLedgerRepository extends JpaRepository<PointLedgerEntity, String> {
    boolean existsBySourceIdAndSourceType(String sourceId, PointLedgerSourceType sourceType);
}