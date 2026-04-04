package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.greenaction.dto.response.GreenActionPostDetailResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostReviewerResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostSummaryResponse;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface GreenActionMapper {

    @Mapping(target = "authorDisplayName", source = ".", qualifiedByName = "getAuthorDisplayName")
    @Mapping(target = "actionTypeName", source = "actionType.actionName")
    @Mapping(target = "groupName", source = "actionType.groupName")
    GreenActionPostSummaryResponse toSummaryResponse(GreenActionPostEntity entity);

    @Mapping(target = "authorDisplayName", source = ".", qualifiedByName = "getAuthorDisplayName")
    @Mapping(target = "actionTypeName", source = "actionType.actionName")
    @Mapping(target = "groupName", source = "actionType.groupName")
    GreenActionPostDetailResponse toDetailResponse(GreenActionPostEntity entity);

    @Mapping(target = "authorDisplayName", source = ".", qualifiedByName = "getAuthorDisplayName")
    @Mapping(target = "actionTypeId", source = "actionType.id")
    @Mapping(target = "actionTypeName", source = "actionType.actionName")
    @Mapping(target = "groupName", source = "actionType.groupName")
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
}
