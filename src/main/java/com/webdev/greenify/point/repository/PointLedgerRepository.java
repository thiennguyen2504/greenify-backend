package com.webdev.greenify.point.repository;

import com.webdev.greenify.point.entity.PointLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLedgerRepository extends JpaRepository<PointLedgerEntity, String> {
}