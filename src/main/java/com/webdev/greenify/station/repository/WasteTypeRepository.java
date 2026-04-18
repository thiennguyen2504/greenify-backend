package com.webdev.greenify.station.repository;

import com.webdev.greenify.station.entity.WasteTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WasteTypeRepository extends JpaRepository<WasteTypeEntity, String> {
    Optional<WasteTypeEntity> findByName(String name);

    List<WasteTypeEntity> findAllByOrderByNameAsc();
}
