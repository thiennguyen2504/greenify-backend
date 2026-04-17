package com.webdev.greenify.leaderboard.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leaderboard_prize_configs")
public class LeaderboardPrizeConfigEntity extends BaseEntity {

    @Column(name = "week_start_date", nullable = false, unique = true)
    private LocalDate weekStartDate;

    @Column(name = "lock_at", nullable = false)
    private LocalDateTime lockAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PrizeConfigStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "national_voucher_template_id", nullable = false)
    private VoucherTemplateEntity nationalVoucherTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincial_voucher_template_id", nullable = false)
    private VoucherTemplateEntity provincialVoucherTemplate;

    @Column(name = "national_reserved_count", nullable = false)
    private Integer nationalReservedCount;

    @Column(name = "provincial_reserved_count", nullable = false)
    private Integer provincialReservedCount;

    @Column(name = "distributed_at")
    private LocalDateTime distributedAt;
}
