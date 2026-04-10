package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.station.mapper.AddressMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {AddressMapper.class, ImageMapper.class})
public interface EventMapper {

    @Mapping(target = "organizer", ignore = true)
    EventEntity toEntity(EventRequestDTO dto);

    EventResponseDTO toResponse(EventEntity entity);
}
