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
public class TrashSpotSummaryResponse {

    private String id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String location;
    private String province;
    private TrashSpotStatus status;
    private SeverityTier severityTier;
    private BigDecimal hotScore;
    private Integer verificationCount;
    private String primaryImageUrl;
    private List<String> wasteTypeNames;
    private LocalDateTime createdAt;
}
