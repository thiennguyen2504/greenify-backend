package com.webdev.greenify.trashspot.mapper;

import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.trashspot.dto.response.ResolveRequestResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotDetailResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotReportResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotReportTrashSpotResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotSummaryResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotVerificationResponse;
import com.webdev.greenify.trashspot.entity.TrashSpotEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotResolveImageEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotReportEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotResolveRequestEntity;
import com.webdev.greenify.trashspot.entity.TrashSpotVerificationEntity;
import com.webdev.greenify.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface TrashSpotMapper {

    @Mapping(target = "primaryImageUrl", source = ".", qualifiedByName = "getPrimaryImageUrl")
    @Mapping(target = "wasteTypeNames", source = ".", qualifiedByName = "getWasteTypeNames")
    TrashSpotSummaryResponse toSummaryResponse(TrashSpotEntity entity);

    @Mapping(target = "reporterId", source = "reporter.id")
    @Mapping(target = "reporterDisplayName", source = ".", qualifiedByName = "getReporterDisplayName")
    @Mapping(target = "assignedNgoId", source = "assignedNgo.id")
    @Mapping(target = "assignedNgoDisplayName", source = ".", qualifiedByName = "getAssignedNgoDisplayName")
    @Mapping(target = "imageUrls", source = ".", qualifiedByName = "getImageUrls")
    @Mapping(target = "wasteTypeIds", source = ".", qualifiedByName = "getWasteTypeIds")
    @Mapping(target = "wasteTypeNames", source = ".", qualifiedByName = "getWasteTypeNames")
    @Mapping(target = "verifications", ignore = true)
    @Mapping(target = "resolveRequests", ignore = true)
    TrashSpotDetailResponse toDetailResponse(TrashSpotEntity entity);

    @Mapping(target = "verifierId", source = "verifier.id")
    @Mapping(target = "verifierDisplayName", source = ".", qualifiedByName = "getVerifierDisplayName")
    TrashSpotVerificationResponse toVerificationResponse(TrashSpotVerificationEntity entity);

    List<TrashSpotVerificationResponse> toVerificationResponseList(List<TrashSpotVerificationEntity> entities);

    @Mapping(target = "trashSpot", source = "trashSpot")
    @Mapping(target = "reporterId", source = "reporter.id")
    @Mapping(target = "reporterDisplayName", source = ".", qualifiedByName = "getReportReporterDisplayName")
    @Mapping(target = "reporterAvatarUrl", source = ".", qualifiedByName = "getReportReporterAvatarUrl")
    TrashSpotReportResponse toReportResponse(TrashSpotReportEntity entity);

    List<TrashSpotReportResponse> toReportResponseList(List<TrashSpotReportEntity> entities);

    @Mapping(target = "primaryImageUrl", source = ".", qualifiedByName = "getPrimaryImageUrl")
    @Mapping(target = "wasteTypeNames", source = ".", qualifiedByName = "getWasteTypeNames")
    TrashSpotReportTrashSpotResponse toReportTrashSpotResponse(TrashSpotEntity entity);

    @Mapping(target = "trashSpotId", source = "trashSpot.id")
    @Mapping(target = "ngoId", source = "ngo.id")
    @Mapping(target = "ngoDisplayName", source = ".", qualifiedByName = "getNgoDisplayName")
    @Mapping(target = "reviewedBy", source = "reviewedBy.id")
    @Mapping(target = "imageUrls", source = ".", qualifiedByName = "getResolveImageUrls")
    ResolveRequestResponse toResolveRequestResponse(TrashSpotResolveRequestEntity entity);

    List<ResolveRequestResponse> toResolveRequestResponseList(List<TrashSpotResolveRequestEntity> entities);

    @Named("getPrimaryImageUrl")
    default String getPrimaryImageUrl(TrashSpotEntity entity) {
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            return null;
        }
        return entity.getImages().get(0).getImageUrl();
    }

    @Named("getImageUrls")
    default List<String> getImageUrls(TrashSpotEntity entity) {
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            return List.of();
        }
        return entity.getImages().stream()
                .map(image -> image == null ? null : image.getImageUrl())
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    @Named("getWasteTypeIds")
    default List<String> getWasteTypeIds(TrashSpotEntity entity) {
        Set<WasteTypeEntity> wasteTypes = entity.getWasteTypes();
        if (wasteTypes == null || wasteTypes.isEmpty()) {
            return List.of();
        }
        return wasteTypes.stream()
                .map(WasteTypeEntity::getId)
                .toList();
    }

    @Named("getWasteTypeNames")
    default List<String> getWasteTypeNames(TrashSpotEntity entity) {
        Set<WasteTypeEntity> wasteTypes = entity.getWasteTypes();
        if (wasteTypes == null || wasteTypes.isEmpty()) {
            return List.of();
        }
        return wasteTypes.stream()
                .map(WasteTypeEntity::getName)
                .toList();
    }

    @Named("getReporterDisplayName")
    default String getReporterDisplayName(TrashSpotEntity entity) {
        return getUserDisplayName(entity.getReporter());
    }

    @Named("getAssignedNgoDisplayName")
    default String getAssignedNgoDisplayName(TrashSpotEntity entity) {
        return getUserDisplayName(entity.getAssignedNgo());
    }

    @Named("getVerifierDisplayName")
    default String getVerifierDisplayName(TrashSpotVerificationEntity entity) {
        return getUserDisplayName(entity.getVerifier());
    }

    @Named("getReportReporterDisplayName")
    default String getReportReporterDisplayName(TrashSpotReportEntity entity) {
        return getUserDisplayName(entity.getReporter());
    }

    @Named("getReportReporterAvatarUrl")
    default String getReportReporterAvatarUrl(TrashSpotReportEntity entity) {
        return getUserAvatarUrl(entity.getReporter());
    }

    @Named("getNgoDisplayName")
    default String getNgoDisplayName(TrashSpotResolveRequestEntity entity) {
        return getUserDisplayName(entity.getNgo());
    }

    @Named("getResolveImageUrls")
    default List<String> getResolveImageUrls(TrashSpotResolveRequestEntity entity) {
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            return List.of();
        }
        return entity.getImages().stream()
                .map(TrashSpotResolveImageEntity::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    default String getUserDisplayName(UserEntity user) {
        if (user == null) {
            return null;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    default String getUserAvatarUrl(UserEntity user) {
        if (user == null || user.getUserProfile() == null || user.getUserProfile().getAvatar() == null) {
            return null;
        }
        return user.getUserProfile().getAvatar().getImageUrl();
    }
}
