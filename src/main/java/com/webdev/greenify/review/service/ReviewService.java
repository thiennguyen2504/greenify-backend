package com.webdev.greenify.review.service;

import com.webdev.greenify.greenaction.dto.response.GreenActionPostReviewerResponse;
import com.webdev.greenify.review.dto.request.SubmitReviewRequest;
import com.webdev.greenify.review.dto.response.SubmitReviewResponse;

public interface ReviewService {

    GreenActionPostReviewerResponse getPostForReview(String postId);

    SubmitReviewResponse submitReview(String postId, SubmitReviewRequest request);
}
