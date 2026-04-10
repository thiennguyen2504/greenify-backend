package com.webdev.greenify.station.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_waste_type")
public class WasteTypeEntity extends BaseEntity {
    @Column
    private String name;

    @Column
    private String description;

    @ManyToMany(mappedBy = "wasteTypes", fetch = FetchType.LAZY)
    private List<RecyclingStationEntity> recyclingStations;
}
