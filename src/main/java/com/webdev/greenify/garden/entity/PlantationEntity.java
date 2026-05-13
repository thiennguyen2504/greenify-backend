package com.webdev.greenify.garden.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.garden.enumeration.PlantationBuilding;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plantations")
public class PlantationEntity extends BaseEntity {

    @Column(name = "seed_id", nullable = false)
    private String seedId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "garden_archive_id", nullable = false, unique = true)
    private String gardenArchiveId;

    @Column(name = "slot_id", nullable = false)
    private String slotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "building", nullable = false)
    private PlantationBuilding building;

    @Column(name = "wilted_at", nullable = false)
    private LocalDateTime wiltedAt;
}
