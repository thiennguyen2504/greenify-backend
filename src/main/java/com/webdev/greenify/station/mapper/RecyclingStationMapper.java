package com.webdev.greenify.station.mapper;

import com.webdev.greenify.station.dto.RecyclingStationRequestDTO;
import com.webdev.greenify.station.dto.RecyclingStationResponseDTO;
import com.webdev.greenify.station.entity.RecyclingStationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = {AddressMapper.class, OpenTimeMapper.class})
public interface RecyclingStationMapper {

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "wasteTypes", ignore = true)
    RecyclingStationEntity toEntity(RecyclingStationRequestDTO dto);

    RecyclingStationResponseDTO toRecyclingStationResponseDTO(RecyclingStationEntity entity);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "wasteTypes", ignore = true)
    void updateEntity(@MappingTarget RecyclingStationEntity entity, RecyclingStationRequestDTO dto);
}
