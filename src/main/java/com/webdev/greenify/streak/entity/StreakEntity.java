package com.webdev.greenify.streak.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.streak.enumeration.StreakStatus;
import com.webdev.greenify.user.entity.UserEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "streaks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_streak_user", columnNames = {"user_id"})
})
@AttributeOverride(name = "lastModifiedAt", column = @Column(name = "updated_at"))
public class StreakEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private UserEntity user;

    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    private Integer longestStreak = 0;

    @Column(name = "last_valid_date")
    private LocalDate lastValidDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StreakStatus status = StreakStatus.NOT_STARTED;

    @Column(name = "restore_used_this_month", nullable = false)
    private Integer restoreUsedThisMonth = 0;

    @Column(name = "restore_month")
    private LocalDate restoreMonth;

    @Column(name = "last_break_date")
    private LocalDate lastBreakDate;

    @Column(name = "broken_streak", nullable = false)
    private Integer brokenStreak = 0;
}
