package com.webdev.greenify.greenaction.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonAlias("reject_reason")
    private String rejectReason;

    @JsonAlias("reject_reason_code")
    private String rejectReasonCode;

    @JsonAlias("reject_reason_note")
    private String rejectReasonNote;

    @JsonIgnore
    public String resolveRejectReason() {
        return trimToNull(rejectReason);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
