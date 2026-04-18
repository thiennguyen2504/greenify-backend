package com.webdev.greenify.user.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.user.enumeration.UserManagementActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = "user")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_management_actions")
public class UserManagementActionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 30, nullable = false)
    private UserManagementActionType actionType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "actor_user_id", nullable = false)
    private String actorUserId;
}