package com.webdev.greenify.garden.mapper;

import com.webdev.greenify.garden.dto.request.UpdateSeedRequest;
import com.webdev.greenify.garden.dto.response.SeedResponse;
import com.webdev.greenify.garden.entity.PlantProgressEntity;
import com.webdev.greenify.garden.entity.SeedEntity;
import com.webdev.greenify.garden.enumeration.PlantStage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SeedMapper {

    @Mapping(target = "rewardVoucherTemplateId", source = "rewardVoucherTemplate.id")
    @Mapping(target = "rewardVoucherName", source = "rewardVoucherTemplate.name")
    SeedResponse toSeedResponse(SeedEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ol", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "stage1ImageUrl", source = "stage1Image.imageUrl")
    @Mapping(target = "stage2ImageUrl", source = "stage2Image.imageUrl")
    @Mapping(target = "stage3ImageUrl", source = "stage3Image.imageUrl")
    @Mapping(target = "stage4ImageUrl", source = "stage4Image.imageUrl")
    @Mapping(target = "rewardVoucherTemplate", ignore = true)
    void updateSeedFromDto(UpdateSeedRequest request, @MappingTarget SeedEntity entity);

    @Named("resolveCurrentStageImageUrl")
    default String resolveCurrentStageImageUrl(PlantProgressEntity progress) {
        if (progress == null || progress.getSeed() == null || progress.getCurrentStage() == null) {
            return null;
        }
        return resolveStageImageUrl(progress.getSeed(), progress.getCurrentStage());
    }

    @Named("resolveStageImageUrl")
    default String resolveStageImageUrl(SeedEntity seed, PlantStage stage) {
        if (seed == null || stage == null) {
            return null;
        }

        return switch (stage) {
            case SEED -> seed.getStage1ImageUrl();
            case SPROUT -> seed.getStage2ImageUrl();
            case GROWING -> seed.getStage3ImageUrl();
            case BLOOMING -> seed.getStage4ImageUrl();
        };
    }
}
