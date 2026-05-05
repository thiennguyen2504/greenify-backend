package com.webdev.greenify.co2e.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.co2e.enumeration.Co2eTransactionStatus;
import com.webdev.greenify.co2e.enumeration.Co2eType;
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

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "co2e_transactions")
public class Co2eTransactionEntity extends BaseEntity {

    @Column(name = "post_id", nullable = false, unique = true)
    private String postId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "co2e_type", length = 20)
    private Co2eType co2eType;

    @Column(name = "co2e_kg", precision = 12, scale = 4)
    private BigDecimal co2eKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Co2eTransactionStatus status;

    @Column(name = "material_code", length = 50)
    private String materialCode;

    @Column(name = "material_label", length = 120)
    private String materialLabel;

    @Column(name = "confidence_score", precision = 6, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "confidence_multiplier", precision = 4, scale = 2)
    private BigDecimal confidenceMultiplier;

    @Column(name = "estimated_weight_kg", precision = 12, scale = 4)
    private BigDecimal estimatedWeightKg;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "skip_reason", length = 120)
    private String skipReason;
}
