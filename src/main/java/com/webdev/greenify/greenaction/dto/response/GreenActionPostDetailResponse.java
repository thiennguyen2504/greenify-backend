package com.webdev.greenify.greenaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
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
public class GreenActionPostDetailResponse {

    private String id;
    private String authorDisplayName;
    private String actionTypeName;
    private String groupName;
    private String caption;
    private String mediaUrl;
    private Integer approveCount;
    private LocalDate actionDate;
    private PostStatus status;
    private LocalDateTime createdAt;
}
