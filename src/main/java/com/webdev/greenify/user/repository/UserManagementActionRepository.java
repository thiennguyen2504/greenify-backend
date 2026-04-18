package com.webdev.greenify.user.repository;

import com.webdev.greenify.user.entity.UserManagementActionEntity;
import com.webdev.greenify.user.enumeration.UserManagementActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserManagementActionRepository extends JpaRepository<UserManagementActionEntity, String> {

    Optional<UserManagementActionEntity> findTopByUser_IdAndActionTypeOrderByCreatedAtDesc(
            String userId,
            UserManagementActionType actionType);
}