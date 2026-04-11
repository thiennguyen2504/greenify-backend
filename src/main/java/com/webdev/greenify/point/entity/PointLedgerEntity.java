package com.webdev.greenify.point.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.point.enumeration.PointLedgerSourceType;
import com.webdev.greenify.point.enumeration.PointLedgerStatus;
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

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "point_ledger")
public class PointLedgerEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserEntity user;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50, nullable = false)
    private PointLedgerSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PointLedgerStatus status;
}