package com.webdev.greenify.leaderboard.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.leaderboard.enumeration.LeaderboardScope;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.voucher.entity.UserVoucherEntity;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leaderboard_snapshots")
public class LeaderboardSnapshotEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_config_id", nullable = false)
    private LeaderboardPrizeConfigEntity prizeConfig;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private LeaderboardScope scope;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "weekly_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal weeklyPoints;

    @Column(name = "rewarded", nullable = false)
    private boolean rewarded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id")
    private UserVoucherEntity userVoucher;
}
