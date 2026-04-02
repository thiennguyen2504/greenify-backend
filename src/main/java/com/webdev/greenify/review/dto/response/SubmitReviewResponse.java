package com.webdev.greenify.review.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.review.enumeration.ReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitReviewResponse {

    private String reviewId;
    private String postId;
    private ReviewDecision decision;
    private PostStatus postStatus;
    private String message;
}
