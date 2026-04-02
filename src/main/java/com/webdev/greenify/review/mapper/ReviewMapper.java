package com.webdev.greenify.review.mapper;

import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.review.dto.response.SubmitReviewResponse;
import com.webdev.greenify.review.entity.PostReviewEntity;
import com.webdev.greenify.review.enumeration.ReviewDecision;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ReviewMapper {

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "postId", source = "review.post.id")
    @Mapping(target = "decision", source = "decision")
    @Mapping(target = "postStatus", source = "postStatus")
    @Mapping(target = "message", constant = "Đã ghi nhận lượt duyệt")
    SubmitReviewResponse toSubmitReviewResponse(PostReviewEntity review, ReviewDecision decision, PostStatus postStatus);
}
