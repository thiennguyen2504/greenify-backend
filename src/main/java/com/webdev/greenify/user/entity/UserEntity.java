package com.webdev.greenify.user.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = {"roles", "userProfile", "events"})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user")
public class UserEntity extends BaseEntity {
    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column
    private String oauthId;

    @OneToOne(mappedBy = "user")
    private UserProfileEntity userProfile;

    @OneToMany(mappedBy = "organizer", fetch = FetchType.LAZY)
    private List<EventEntity> events = new ArrayList<>();
}
