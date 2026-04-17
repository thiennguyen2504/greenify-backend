package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.SubmitReviewRequest;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostReviewerResponse;
import com.webdev.greenify.greenaction.dto.response.SubmitReviewResponse;

public interface ReviewService {

    GreenActionPostReviewerResponse getPostForReview(String postId);

    SubmitReviewResponse submitReview(String postId, SubmitReviewRequest request);
}
