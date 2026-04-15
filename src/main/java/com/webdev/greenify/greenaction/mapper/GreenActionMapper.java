package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.greenaction.dto.response.GreenActionPostDetailResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostReviewerResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostSummaryResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface GreenActionMapper {

    @Mapping(target = "authorDisplayName", source = ".", qualifiedByName = "getAuthorDisplayName")
    @Mapping(target = "authorAvatarUrl", source = ".", qualifiedByName = "getAuthorAvatarUrl")
    @Mapping(target = "actionTypeName", source = "actionType.actionName")
    @Mapping(target = "groupName", source = "actionType.groupName")
    @Mapping(target = "mediaUrl", source = ".", qualifiedByName = "getMediaUrl")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    GreenActionPostSummaryResponse toSummaryResponse(GreenActionPostEntity entity);

    @Mapping(target = "authorDisplayName", source = ".", qualifiedByName = "getAuthorDisplayName")
    @Mapping(target = "authorAvatarUrl", source = ".", qualifiedByName = "getAuthorAvatarUrl")
    @Mapping(target = "actionTypeName", source = "actionType.actionName")
    @Mapping(target = "groupName", source = "actionType.groupName")
    @Mapping(target = "mediaUrl", source = ".", qualifiedByName = "getMediaUrl")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    GreenActionPostDetailResponse toDetailResponse(GreenActionPostEntity entity);

    @Mapping(target = "authorDisplayName", source = ".", qualifiedByName = "getAuthorDisplayName")
    @Mapping(target = "authorAvatarUrl", source = ".", qualifiedByName = "getAuthorAvatarUrl")
    @Mapping(target = "actionTypeId", source = "actionType.id")
    @Mapping(target = "actionTypeName", source = "actionType.actionName")
    @Mapping(target = "groupName", source = "actionType.groupName")
    @Mapping(target = "mediaUrl", source = ".", qualifiedByName = "getMediaUrl")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "alreadyReviewed", ignore = true)
    GreenActionPostReviewerResponse toReviewerResponse(GreenActionPostEntity entity);

    @Named("getAuthorDisplayName")
    default String getAuthorDisplayName(GreenActionPostEntity entity) {
        if (entity.getUser() == null) {
            return null;
        }
        // Prefer username, fallback to email
        String username = entity.getUser().getUsername();
        if (username != null && !username.isBlank()) {
            return username;
        }
        return entity.getUser().getEmail();
    }

    @Named("getAuthorAvatarUrl")
    default String getAuthorAvatarUrl(GreenActionPostEntity entity) {
        if (entity.getUser() == null
                || entity.getUser().getUserProfile() == null
                || entity.getUser().getUserProfile().getAvatar() == null) {
            return null;
        }
        return entity.getUser().getUserProfile().getAvatar().getImageUrl();
    }

    @Named("getMediaUrl")
    default String getMediaUrl(GreenActionPostEntity entity) {
        if (entity.getPostImage() == null) {
            return null;
        }
        return entity.getPostImage().getImageUrl();
    }
}
