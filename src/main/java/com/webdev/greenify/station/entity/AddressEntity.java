package com.webdev.greenify.station.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.user.entity.NGOProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
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

    @OneToOne(mappedBy = "address")
    private EventEntity event;

    @OneToOne(mappedBy = "address")
    private NGOProfileEntity ngoProfile;
}
