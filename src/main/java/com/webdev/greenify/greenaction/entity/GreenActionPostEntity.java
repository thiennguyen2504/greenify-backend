package com.webdev.greenify.greenaction.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.file.entity.PostImageEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "green_action_posts")
public class GreenActionPostEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_type_id", nullable = false)
    private GreenActionTypeEntity actionType;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private PostImageEntity postImage;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "action_date")
    private LocalDate actionDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PostStatus status;

    @Column(name = "approve_count", nullable = false)
    private Integer approveCount = 0;

    @Column(name = "reject_count", nullable = false)
    private Integer rejectCount = 0;
}
