package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;
import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.user.mapper.UserProfileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserProfileMapper.class})
public interface EventRegistrationMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "userProfile", source = "user.userProfile")
    EventRegistrationResponseDTO toResponse(EventRegistrationEntity entity);
}
