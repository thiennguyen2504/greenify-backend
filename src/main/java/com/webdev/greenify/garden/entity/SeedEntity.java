package com.webdev.greenify.garden.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
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

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seeds")
public class SeedEntity extends BaseEntity {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "stage1_image_url", columnDefinition = "TEXT", nullable = false)
    private String stage1ImageUrl;

    @Column(name = "stage2_image_url", columnDefinition = "TEXT", nullable = false)
    private String stage2ImageUrl;

    @Column(name = "stage3_image_url", columnDefinition = "TEXT", nullable = false)
    private String stage3ImageUrl;

    @Column(name = "stage4_image_url", columnDefinition = "TEXT", nullable = false)
    private String stage4ImageUrl;

    @Column(name = "days_to_mature", nullable = false)
    private Integer daysToMature;

    @Column(name = "stage2_from_day", nullable = false)
    private Integer stage2FromDay;

    @Column(name = "stage3_from_day", nullable = false)
    private Integer stage3FromDay;

    @Column(name = "stage4_from_day", nullable = false)
    private Integer stage4FromDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_type", length = 30, nullable = false)
    private PlantCycleType cycleType = PlantCycleType.SHORT_TERM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_voucher_template_id")
    @ToString.Exclude
    private VoucherTemplateEntity rewardVoucherTemplate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
