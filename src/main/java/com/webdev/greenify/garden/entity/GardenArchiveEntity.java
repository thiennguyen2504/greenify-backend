package com.webdev.greenify.garden.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.garden.enumeration.GardenRewardStatus;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.voucher.entity.UserVoucherEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "garden_archives")
public class GardenArchiveEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seed_id", nullable = false)
    @ToString.Exclude
    private SeedEntity seed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_progress_id", nullable = false)
    @ToString.Exclude
    private PlantProgressEntity plantProgress;

    @Column(name = "days_taken", nullable = false)
    private Integer daysTaken;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_status", length = 20, nullable = false)
    private GardenRewardStatus rewardStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id")
    @ToString.Exclude
    private UserVoucherEntity userVoucher;

    @Column(name = "display_image_url", columnDefinition = "TEXT", nullable = false)
    private String displayImageUrl;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
}
