package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.greenaction.dto.response.SubmitReviewResponse;
import com.webdev.greenify.greenaction.entity.PostReviewEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.enumeration.ReviewDecision;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "postId", source = "review.post.id")
    @Mapping(target = "decision", source = "decision")
    @Mapping(target = "postStatus", source = "postStatus")
    @Mapping(target = "message", constant = "Đã ghi nhận lượt duyệt")
    SubmitReviewResponse toSubmitReviewResponse(PostReviewEntity review, ReviewDecision decision, PostStatus postStatus);
}
