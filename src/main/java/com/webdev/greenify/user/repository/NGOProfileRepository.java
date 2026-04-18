package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.NGOProfileEntity;
import com.webdev.greenify.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NGOProfileRepository extends JpaRepository<NGOProfileEntity, String>, JpaSpecificationExecutor<NGOProfileEntity> {
    Optional<NGOProfileEntity> findByUser(UserEntity user);
    Optional<NGOProfileEntity> findByUserId(String userId);
}
