package com.webdev.greenify.user.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.file.entity.ProfileImageEntity;
import com.webdev.greenify.user.enumeration.UserProfileStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_profile")
public class UserProfileEntity extends BaseEntity {
    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String displayName;

    @Column
    private String province;

    @Column
    private String district;

    @Column
    private String ward;

    @Column
    private String addressDetail;

    @Column
    private UserProfileStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @OneToOne(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProfileImageEntity avatar;
}
