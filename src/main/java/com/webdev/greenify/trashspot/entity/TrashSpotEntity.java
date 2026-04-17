package com.webdev.greenify.trashspot.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.trashspot.enumeration.SeverityTier;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;
import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(
        callSuper = true,
        exclude = {"reporter", "assignedNgo", "images", "wasteTypes", "verifications", "resolveRequests"})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trash_spots")
public class TrashSpotEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(length = 100, nullable = false)
    private String province;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TrashSpotStatus status;

    @Builder.Default
    @Column(name = "verification_count", nullable = false)
    private Integer verificationCount = 0;

    @Builder.Default
    @Column(name = "hot_score", precision = 8, scale = 4, nullable = false)
    private BigDecimal hotScore = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_tier", length = 20, nullable = false)
    private SeverityTier severityTier = SeverityTier.SEVERITY_LOW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_ngo_id")
    private UserEntity assignedNgo;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "trashSpot", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<TrashSpotImageEntity> images = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "trash_spot_waste_types",
            joinColumns = @JoinColumn(name = "trash_spot_id"),
            inverseJoinColumns = @JoinColumn(name = "waste_type_id"))
    @ToString.Exclude
    @Builder.Default
    private Set<WasteTypeEntity> wasteTypes = new HashSet<>();

    @OneToMany(mappedBy = "trashSpot", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<TrashSpotVerificationEntity> verifications = new ArrayList<>();

    @OneToMany(mappedBy = "trashSpot", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<TrashSpotResolveRequestEntity> resolveRequests = new ArrayList<>();
}
