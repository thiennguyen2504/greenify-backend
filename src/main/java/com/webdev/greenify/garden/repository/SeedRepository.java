package com.webdev.greenify.garden.repository;

import com.webdev.greenify.garden.entity.SeedEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeedRepository extends JpaRepository<SeedEntity, String> {

    @EntityGraph(attributePaths = {"rewardVoucherTemplate"})
    List<SeedEntity> findAllByIsActiveTrue();

    @EntityGraph(attributePaths = {"rewardVoucherTemplate"})
    Page<SeedEntity> findAllByIsActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"rewardVoucherTemplate"})
    Optional<SeedEntity> findByIdAndIsActiveTrue(String id);
}
