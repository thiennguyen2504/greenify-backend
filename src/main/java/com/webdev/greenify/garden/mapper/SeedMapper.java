package com.webdev.greenify.garden.mapper;

import com.webdev.greenify.garden.dto.response.SeedResponse;
import com.webdev.greenify.garden.entity.PlantProgressEntity;
import com.webdev.greenify.garden.entity.SeedEntity;
import com.webdev.greenify.garden.enumeration.PlantStage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SeedMapper {

    @Mapping(target = "rewardVoucherTemplateId", source = "rewardVoucherTemplate.id")
    @Mapping(target = "rewardVoucherName", source = "rewardVoucherTemplate.name")
    SeedResponse toSeedResponse(SeedEntity entity);

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
