package com.webdev.greenify.user.mapper;

import com.webdev.greenify.user.dto.UserProfileCreateRequestDTO;
import com.webdev.greenify.user.dto.UserProfileResponseDTO;
import com.webdev.greenify.user.dto.UserProfileUpdateRequestDTO;
import com.webdev.greenify.user.entity.UserProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "avatarUrl", source = "avatar.imageUrl")
    UserProfileResponseDTO toDto(UserProfileEntity entity);

    @Mapping(target = "avatar", ignore = true)
    UserProfileEntity toEntity(UserProfileCreateRequestDTO dto);

    @Mapping(target = "avatar", ignore = true)
    void updateProfileFromDto(UserProfileUpdateRequestDTO dto, @MappingTarget UserProfileEntity entity);
}

