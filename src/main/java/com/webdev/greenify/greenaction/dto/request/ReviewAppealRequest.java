package com.webdev.greenify.greenaction.dto.request;

import com.webdev.greenify.greenaction.enumeration.AppealStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAppealRequest {

    @NotNull(message = "Review status is required")
    private AppealStatus status;

    private String adminNote;
}
