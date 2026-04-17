package com.webdev.greenify.trashspot.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"trashSpot", "images"})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trash_spot_resolve_requests")
public class TrashSpotResolveRequestEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trash_spot_id", nullable = false)
    @ToString.Exclude
    private TrashSpotEntity trashSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngo_id", nullable = false)
    private UserEntity ngo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "cleaned_at", nullable = false)
    private LocalDateTime cleanedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResolveRequestStatus status;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "resolveRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<TrashSpotResolveImageEntity> images = new ArrayList<>();
}
