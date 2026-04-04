package com.webdev.greenify.greenaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GreenActionPostSummaryResponse {

    private String id;
    private String authorDisplayName;
    private String actionTypeName;
    private String groupName;
    private String caption;
    private String mediaUrl;
    private Integer approveCount;
    private LocalDate actionDate;
    private LocalDateTime createdAt;
}
