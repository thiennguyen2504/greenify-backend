package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GreenActionTypeRepository extends JpaRepository<GreenActionTypeEntity, String> {

    Optional<GreenActionTypeEntity> findByIdAndIsActiveTrue(String id);

    List<GreenActionTypeEntity> findAllByOrderByGroupNameAscActionNameAsc();
}
