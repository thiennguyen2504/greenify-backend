package com.webdev.greenify.voucher.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;
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
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "voucher_templates")
public class VoucherTemplateEntity extends BaseEntity {

    @Column(length = 200, nullable = false)
    private String name;

    @Column(name = "partner_name", length = 200, nullable = false)
    private String partnerName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_points", precision = 10, scale = 2, nullable = false)
    private BigDecimal requiredPoints;

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;

    @Column(name = "remaining_stock", nullable = false)
    private Integer remainingStock;

    @Column(name = "usage_conditions", columnDefinition = "TEXT")
    private String usageConditions;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "partner_logo_url", columnDefinition = "TEXT")
    private String partnerLogoUrl;

    @Column(name = "partner_logo_bucket", length = 200)
    private String partnerLogoBucket;

    @Column(name = "partner_logo_object_key", columnDefinition = "TEXT")
    private String partnerLogoObjectKey;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "thumbnail_bucket", length = 200)
    private String thumbnailBucket;

    @Column(name = "thumbnail_object_key", columnDefinition = "TEXT")
    private String thumbnailObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private VoucherTemplateStatus status;
}