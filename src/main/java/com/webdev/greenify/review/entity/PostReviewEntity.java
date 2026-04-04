package com.webdev.greenify.review.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.review.enumeration.ReviewDecision;
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
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_reviews")
public class PostReviewEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private GreenActionPostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private UserEntity reviewer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewDecision decision;

    @Column(name = "reject_reason_code", length = 50)
    private String rejectReasonCode;

    @Column(name = "reject_reason_note", columnDefinition = "TEXT")
    private String rejectReasonNote;

    @Column(name = "is_valid")
    private Boolean isValid;
}
