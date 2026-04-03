package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {
    Optional<RoleEntity> findByName(String name);

    @Query("""
        SELECT r FROM RoleEntity r
        WHERE r.name IN :name
    """)
    Set<RoleEntity> findAllByNames(Set<String> name);
}
