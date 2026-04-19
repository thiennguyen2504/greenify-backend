package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {
    @Override
    @EntityGraph(attributePaths = {"roles", "userProfile", "userProfile.avatar"})
    Page<UserEntity> findAll(Specification<UserEntity> spec, Pageable pageable);

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
        LEFT JOIN FETCH u.ngoProfile np
        LEFT JOIN FETCH np.address
        LEFT JOIN FETCH np.avatar
        LEFT JOIN FETCH np.verificationDocs
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
