package com.webdev.greenify.greenaction.dto.request;

import com.webdev.greenify.greenaction.enumeration.ReviewDecision;
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

    @NotNull(message = "Quyết định là bắt buộc")
    private ReviewDecision decision;

    private String rejectReason;
}
