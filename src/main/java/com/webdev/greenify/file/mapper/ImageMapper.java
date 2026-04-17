package com.webdev.greenify.file.mapper;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.file.entity.EventImageEntity;
import com.webdev.greenify.file.entity.NGODocsImageEntity;
import com.webdev.greenify.file.entity.NGOProfileImageEntity;
import com.webdev.greenify.file.entity.PostImageEntity;
import com.webdev.greenify.file.entity.ProfileImageEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    ProfileImageEntity toProfileImageEntity(ImageRequestDTO dto);

    NGOProfileImageEntity toNGOProfileImageEntity(ImageRequestDTO dto);

    NGODocsImageEntity toNGODocsImageEntity(ImageRequestDTO dto);

    PostImageEntity toPostImageEntity(ImageRequestDTO dto);

    EventImageEntity toEventImageEntity(ImageRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileImage(ImageRequestDTO dto, @MappingTarget ProfileImageEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateNGOProfileImage(ImageRequestDTO dto, @MappingTarget NGOProfileImageEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePostImage(ImageRequestDTO dto, @MappingTarget PostImageEntity entity);
}
