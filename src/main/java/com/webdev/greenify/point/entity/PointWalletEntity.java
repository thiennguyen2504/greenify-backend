package com.webdev.greenify.point.entity;

import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "point_wallets")
public class PointWalletEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "available_points", precision = 10, scale = 2, nullable = false)
    private BigDecimal availablePoints;

    @Column(name = "total_points", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPoints;

    @Column(name = "weekly_points", precision = 10, scale = 2)
    private BigDecimal weeklyPoints;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}