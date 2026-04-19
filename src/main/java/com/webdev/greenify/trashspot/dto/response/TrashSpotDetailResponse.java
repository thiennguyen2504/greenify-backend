package com.webdev.greenify.trashspot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.trashspot.enumeration.SeverityTier;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrashSpotDetailResponse {

    private String id;
    private String name;
    private String reporterId;
    private String reporterDisplayName;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String location;
    private String province;
    private TrashSpotStatus status;
    private Integer verificationCount;
    private BigDecimal hotScore;
    private SeverityTier severityTier;
    private String assignedNgoId;
    private String assignedNgoDisplayName;
    private LocalDateTime claimedAt;
    private LocalDateTime resolvedAt;
    private List<String> imageUrls;
    private List<String> wasteTypeIds;
    private List<String> wasteTypeNames;
    private List<TrashSpotVerificationResponse> verifications;
    private List<ResolveRequestResponse> resolveRequests;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
