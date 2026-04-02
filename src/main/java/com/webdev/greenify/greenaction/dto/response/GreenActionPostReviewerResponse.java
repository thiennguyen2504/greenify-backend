package com.webdev.greenify.greenaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GreenActionPostReviewerResponse {

    private String id;
    private String authorDisplayName;
    private String actionTypeId;
    private String actionTypeName;
    private String groupName;
    private String caption;
    private String mediaUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer approveCount;
    private Integer rejectCount;
    private LocalDate actionDate;
    private PostStatus status;
    private LocalDateTime createdAt;
    private Boolean alreadyReviewed;
}
