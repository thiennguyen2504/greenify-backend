package com.webdev.greenify.user.mapper;

import com.webdev.greenify.user.dto.UserDetailResponseDTO;
import com.webdev.greenify.user.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDetailResponseDTO toDto(UserEntity userEntity);
}
