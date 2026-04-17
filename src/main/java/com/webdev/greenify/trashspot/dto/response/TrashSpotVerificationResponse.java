package com.webdev.greenify.trashspot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrashSpotVerificationResponse {

    private String id;
    private String verifierId;
    private String verifierDisplayName;
    private String note;
    private LocalDateTime createdAt;
}
