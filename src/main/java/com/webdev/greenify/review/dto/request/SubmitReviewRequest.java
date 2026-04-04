package com.webdev.greenify.review.dto.request;

import com.webdev.greenify.review.enumeration.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitReviewRequest {

    @NotNull(message = "Decision is required")
    private ReviewDecision decision;

    private String rejectReasonCode;

    private String rejectReasonNote;
}
