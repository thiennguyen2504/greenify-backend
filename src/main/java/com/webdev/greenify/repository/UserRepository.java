package com.webdev.greenify.repository;

import com.webdev.greenify.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.roles")
    List<UserEntity> findAllWithRoles();

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
