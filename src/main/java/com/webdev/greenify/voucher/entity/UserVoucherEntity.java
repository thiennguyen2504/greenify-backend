package com.webdev.greenify.voucher.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.voucher.enumeration.UserVoucherStatus;
import com.webdev.greenify.voucher.enumeration.VoucherSource;
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

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_vouchers")
public class UserVoucherEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_template_id", nullable = false)
    @ToString.Exclude
    private VoucherTemplateEntity voucherTemplate;

    @Column(name = "voucher_code", length = 100, nullable = false, unique = true)
    private String voucherCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private VoucherSource source;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserVoucherStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}