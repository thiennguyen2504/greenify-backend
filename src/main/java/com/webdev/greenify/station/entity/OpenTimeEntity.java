package com.webdev.greenify.station.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_open_time")
public class OpenTimeEntity extends BaseEntity {
    @Column
    private LocalTime startTime;

    @Column
    private LocalTime endTime;

    @Column
    private DayOfWeek dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "recycling_station_id", referencedColumnName = "id")
    private RecyclingStationEntity recyclingStation;
}
