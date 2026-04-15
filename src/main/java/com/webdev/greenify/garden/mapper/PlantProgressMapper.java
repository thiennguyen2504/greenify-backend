package com.webdev.greenify.garden.mapper;

import com.webdev.greenify.garden.dto.response.PlantProgressResponse;
import com.webdev.greenify.garden.entity.PlantProgressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = SeedMapper.class)
public interface PlantProgressMapper {

    @Mapping(target = "seedId", source = "seed.id")
    @Mapping(target = "seedName", source = "seed.name")
    @Mapping(target = "daysToMature", source = "seed.daysToMature")
    @Mapping(target = "cycleType", source = "seed.cycleType")
    @Mapping(target = "currentStageImageUrl", source = ".", qualifiedByName = "resolveCurrentStageImageUrl")
    @Mapping(target = "percentComplete", source = ".", qualifiedByName = "calculatePercentComplete")
    PlantProgressResponse toPlantProgressResponse(PlantProgressEntity entity);

    @Named("calculatePercentComplete")
    default Double calculatePercentComplete(PlantProgressEntity entity) {
        if (entity == null || entity.getSeed() == null || entity.getSeed().getDaysToMature() == null
                || entity.getSeed().getDaysToMature() <= 0) {
            return 0D;
        }

        int progressDays = valueOrZero(entity.getProgressDays());
        double percent = (double) progressDays / entity.getSeed().getDaysToMature() * 100D;
        return Math.min(Math.max(percent, 0D), 100D);
    }

    default int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }
}
