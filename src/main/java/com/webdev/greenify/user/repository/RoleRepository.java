package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {
    Optional<RoleEntity> findByName(String name);
}
