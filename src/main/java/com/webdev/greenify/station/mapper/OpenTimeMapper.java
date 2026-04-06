package com.webdev.greenify.station.mapper;

import com.webdev.greenify.station.dto.OpenTimeRequestDTO;
import com.webdev.greenify.station.dto.OpenTimeResponseDTO;
import com.webdev.greenify.station.entity.OpenTimeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface OpenTimeMapper {

    @Mapping(target = "recyclingStation", ignore = true)
    OpenTimeEntity toEntity(OpenTimeRequestDTO dto);

    OpenTimeResponseDTO toResponseDTO(OpenTimeEntity entity);
}
