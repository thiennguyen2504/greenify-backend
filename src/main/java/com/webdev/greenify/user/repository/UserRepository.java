package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.roles")
    List<UserEntity> findAllWithRoles();

    @Query("""
    SELECT u
    FROM UserEntity u
    WHERE (u.username = :identifier OR u.email = :identifier OR u.phoneNumber = :identifier) AND u.status = 'ACTIVE'
    """)
    Optional<UserEntity> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByEmail(String email);
}
