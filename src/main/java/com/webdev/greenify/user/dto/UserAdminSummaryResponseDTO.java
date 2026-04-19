package com.webdev.greenify.user.dto;

import com.webdev.greenify.user.enumeration.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminSummaryResponseDTO {
    private String id;
    private String name;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private String email;
    private String phoneNumber;
    private Set<String> roles;
    private AccountStatus status;
    private BigDecimal availableGreenPoints;
    private long greenPostCount;
    private String suspensionReason;
    private UserProfileResponseDTO userProfile;
    private NGOProfileResponseDTO ngoProfile;
}