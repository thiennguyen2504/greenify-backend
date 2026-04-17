package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {
    Optional<UserProfileEntity> findByUserId(String userId);

    @Query("""
            SELECT DISTINCT TRIM(up.province)
            FROM UserProfileEntity up
            WHERE up.province IS NOT NULL
            AND TRIM(up.province) <> ''
            """)
    List<String> findDistinctProvincesForLeaderboard();
}

