package com.webdev.greenify.trashspot.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"trashSpot", "reporter"})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "trash_spot_reports",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tsr_spot_reporter", columnNames = {"trash_spot_id", "reporter_id"})
        })
public class TrashSpotReportEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trash_spot_id", nullable = false)
    @ToString.Exclude
    private TrashSpotEntity trashSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;
}