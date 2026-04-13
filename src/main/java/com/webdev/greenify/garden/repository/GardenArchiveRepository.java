package com.webdev.greenify.garden.repository;

import com.webdev.greenify.garden.entity.GardenArchiveEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GardenArchiveRepository extends JpaRepository<GardenArchiveEntity, String> {

    @EntityGraph(attributePaths = {"seed", "userVoucher", "userVoucher.voucherTemplate"})
    List<GardenArchiveEntity> findByUserIdOrderByArchivedAtDesc(String userId);

    @EntityGraph(attributePaths = {"seed", "userVoucher", "userVoucher.voucherTemplate"})
    Page<GardenArchiveEntity> findByUserIdOrderByArchivedAtDesc(String userId, Pageable pageable);
}
