package com.webdev.greenify.trashspot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResolveRequestResponse {

    private String id;
    private String trashSpotId;
    private String ngoId;
    private String ngoDisplayName;
    private String description;
    private LocalDateTime cleanedAt;
    private ResolveRequestStatus status;
    private String rejectReason;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
