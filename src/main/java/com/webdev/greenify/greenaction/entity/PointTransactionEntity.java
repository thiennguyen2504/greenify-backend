package com.webdev.greenify.greenaction.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "point_transactions")
public class PointTransactionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "points", precision = 10, scale = 2, nullable = false)
    private BigDecimal points;

    @Column(name = "action_description", length = 200, nullable = false)
    private String actionDescription;

    @Column(name = "source_post_id")
    private String sourcePostId;

    @Column(name = "source_review_id")
    private String sourceReviewId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "expired_transaction_id")
    private String expiredTransactionId;
}
