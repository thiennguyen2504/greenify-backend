package com.webdev.greenify.garden.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.garden.enumeration.PlantStage;
import com.webdev.greenify.garden.enumeration.PlantStatus;
import com.webdev.greenify.user.entity.UserEntity;
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
@Table(name = "plant_progresses")
public class PlantProgressEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seed_id", nullable = false)
    @ToString.Exclude
    private SeedEntity seed;

    @Column(name = "progress_days", nullable = false)
    private Integer progressDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", length = 30, nullable = false)
    private PlantStage currentStage = PlantStage.SEED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PlantStatus status = PlantStatus.GROWING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "matured_at")
    private LocalDateTime maturedAt;
}
