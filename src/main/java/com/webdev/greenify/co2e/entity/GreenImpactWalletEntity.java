package com.webdev.greenify.co2e.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "green_impact_wallets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_green_impact_wallet_user", columnNames = "user_id")
})
public class GreenImpactWalletEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "total_avoided_kg", precision = 12, scale = 4, nullable = false)
    private BigDecimal totalAvoidedKg;

    @Column(name = "total_absorbed_kg", precision = 12, scale = 4, nullable = false)
    private BigDecimal totalAbsorbedKg;
}
