package com.webdev.greenify.review.controller;

import com.webdev.greenify.greenaction.dto.response.GreenActionPostReviewerResponse;
import com.webdev.greenify.review.dto.request.SubmitReviewRequest;
import com.webdev.greenify.review.dto.response.SubmitReviewResponse;
import com.webdev.greenify.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/review/posts")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{postId}")
    @PreAuthorize("hasAnyRole('CTV', 'ADMIN')")
    public ResponseEntity<GreenActionPostReviewerResponse> getPostForReview(@PathVariable String postId) {
        return ResponseEntity.ok(reviewService.getPostForReview(postId));
    }

    @PostMapping("/{postId}/reviews")
    @PreAuthorize("hasAnyRole('CTV', 'ADMIN')")
    public ResponseEntity<SubmitReviewResponse> submitReview(
            @PathVariable String postId,
            @Valid @RequestBody SubmitReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.submitReview(postId, request));
    }
}
