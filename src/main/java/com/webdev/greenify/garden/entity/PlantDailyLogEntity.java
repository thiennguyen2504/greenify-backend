package com.webdev.greenify.garden.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.garden.enumeration.PlantStage;
import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plant_daily_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plant_daily_log_user_date", columnNames = {"user_id", "log_date"})
})
public class PlantDailyLogEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_progress_id", nullable = false)
    @ToString.Exclude
    private PlantProgressEntity plantProgress;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PlantStage stage;

    @Column(name = "is_active_day", nullable = false)
    private Boolean isActiveDay = false;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "green_post_url", columnDefinition = "TEXT")
    private String greenPostUrl;
}
