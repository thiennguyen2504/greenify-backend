package com.webdev.greenify.file.mapper;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.file.entity.ProfileImageEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    ProfileImageEntity toEntity(ImageRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ImageRequestDTO dto, @MappingTarget ProfileImageEntity entity);
}
