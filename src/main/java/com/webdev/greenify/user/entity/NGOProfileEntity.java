package com.webdev.greenify.user.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.file.entity.NGODocsImageEntity;
import com.webdev.greenify.file.entity.NGOProfileImageEntity;
import com.webdev.greenify.station.entity.AddressEntity;
import com.webdev.greenify.user.enumeration.NGOProfileStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = {"user", "avatar", "address", "verificationDocs"})
@ToString(callSuper = true, exclude = {"user", "avatar", "address", "verificationDocs"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ngo_profile")
public class NGOProfileEntity extends BaseEntity {
    @Column(nullable = false)
    private String orgName;

    @Column(nullable = false)
    private String representativeName;

    @Column(nullable = false)
    private String hotline;

    @Column(nullable = false)
    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    @Lob
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private NGOProfileStatus status;

    @Column(columnDefinition = "TEXT")
    @Lob
    private String rejectedReason;

    @Column
    private Integer rejectedCount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @OneToOne(mappedBy = "ngoProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private NGOProfileImageEntity avatar;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private AddressEntity address;

    @OneToMany(mappedBy = "ngoProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NGODocsImageEntity> verificationDocs = new ArrayList<>();
}
