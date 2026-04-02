package com.webdev.greenify.greenaction.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "green_action_types")
public class GreenActionTypeEntity extends BaseEntity {

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(name = "action_name", length = 200)
    private String actionName;

    @Column(name = "suggested_points", precision = 6, scale = 2)
    private BigDecimal suggestedPoints;

    @Column(name = "location_required")
    private Boolean locationRequired;

    @Column(name = "is_active")
    private Boolean isActive;
}
