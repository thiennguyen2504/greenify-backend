package com.webdev.greenify.mapper;

import com.webdev.greenify.dto.UserDetailResponseDTO;
import com.webdev.greenify.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDetailResponseDTO toDto(UserEntity userEntity);
}
