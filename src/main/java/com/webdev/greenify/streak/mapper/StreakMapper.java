package com.webdev.greenify.streak.mapper;

import com.webdev.greenify.streak.dto.response.StreakResponse;
import com.webdev.greenify.streak.entity.StreakEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StreakMapper {

    @Mapping(target = "restoreAvailable", ignore = true)
    StreakResponse toStreakResponse(StreakEntity entity);
}
