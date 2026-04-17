package com.webdev.greenify.greenaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.greenaction.enumeration.AppealStatus;
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
public class AppealResponse {

    private String id;
    private String postId;
    private String userId;
    private String appealReason;
    private List<String> evidenceUrls;
    private Integer attemptNumber;
    private AppealStatus status;
    private String adminNote;
    private LocalDateTime createdAt;
}
