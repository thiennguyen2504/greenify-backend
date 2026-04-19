package com.webdev.greenify.user.mapper;

import com.webdev.greenify.user.dto.UserDetailResponseDTO;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(uses = UserProfileMapper.class)
public interface UserMapper {
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoleNames")
    @Mapping(target = "userProfile", source = "userProfile")
    @Mapping(target = "ngoProfile", source = "ngoProfile")
    UserDetailResponseDTO toDto(UserEntity userEntity);

    @Named("mapRoleNames")
    default Set<String> mapRoleNames(Set<RoleEntity> roles) {
        if (roles == null) return null;

        return roles.stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
    }
}
