package com.webdev.greenify.station.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_address")
public class AddressEntity extends BaseEntity {
    @Column
    private String province;

    @Column
    private String district;

    @Column
    private String ward;

    @Column
    private String addressDetail;

    @Column
    private BigDecimal latitude;

    @Column
    private BigDecimal longitude;

    @OneToOne(mappedBy = "address")
    private RecyclingStationEntity recyclingStation;
}
