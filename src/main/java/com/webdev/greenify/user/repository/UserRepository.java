package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    @Query("""
        SELECT DISTINCT u FROM UserEntity u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH u.userProfile up
        LEFT JOIN FETCH up.avatar
        """)
    List<UserEntity> findAllWithDetails();

    @Query("""
        SELECT DISTINCT u FROM UserEntity u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH u.userProfile up
        LEFT JOIN FETCH up.avatar
        WHERE u.id = :id
        """)
    Optional<UserEntity> findByIdWithDetails(@Param("id") String id);

    @Query("""
        SELECT u
        FROM UserEntity u
        WHERE u.username = :identifier OR u.email = :identifier OR u.phoneNumber = :identifier
        """)
    Optional<UserEntity> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByEmail(String email);
}
