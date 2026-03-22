package com.webdev.greenify.mapper;

import com.webdev.greenify.dto.UserDto;
import com.webdev.greenify.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
