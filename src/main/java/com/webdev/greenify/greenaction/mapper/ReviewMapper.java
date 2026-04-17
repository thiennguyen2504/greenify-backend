package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.greenaction.dto.response.PostReviewResponse;
import com.webdev.greenify.greenaction.dto.response.SubmitReviewResponse;
import com.webdev.greenify.greenaction.entity.PostReviewEntity;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.enumeration.ReviewDecision;
import com.webdev.greenify.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "postId", source = "review.post.id")
    @Mapping(target = "decision", source = "decision")
    @Mapping(target = "postStatus", source = "postStatus")
    @Mapping(target = "message", constant = "Đã ghi nhận lượt duyệt")
    SubmitReviewResponse toSubmitReviewResponse(PostReviewEntity review, ReviewDecision decision, PostStatus postStatus);

    @Mapping(target = "reviewId", source = "id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewerDisplayName", source = "reviewer", qualifiedByName = "toReviewerDisplayName")
    PostReviewResponse toPostReviewResponse(PostReviewEntity review);

    List<PostReviewResponse> toPostReviewResponseList(List<PostReviewEntity> reviews);

    @Named("toReviewerDisplayName")
    default String toReviewerDisplayName(UserEntity reviewer) {
        if (reviewer == null) {
            return null;
        }
        String username = reviewer.getUsername();
        if (username != null && !username.isBlank()) {
            return username;
        }
        return reviewer.getEmail();
    }
}
