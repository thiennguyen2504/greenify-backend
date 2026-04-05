package com.webdev.greenify.station.mapper;

import com.webdev.greenify.station.dto.RecyclingStationRequestDTO;
import com.webdev.greenify.station.dto.RecyclingStationResponseDTO;
import com.webdev.greenify.station.entity.RecyclingStationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(uses = {AddressMapper.class})
public interface RecyclingStationMapper {

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "wasteTypes", ignore = true)
    RecyclingStationEntity toEntity(RecyclingStationRequestDTO dto);

    RecyclingStationResponseDTO toRecyclingStationResponseDTO(RecyclingStationEntity entity);

    List<RecyclingStationResponseDTO> toRecyclingStationResponseDTOList(List<RecyclingStationEntity> entities);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "wasteTypes", ignore = true)
    void updateEntity(@MappingTarget RecyclingStationEntity entity, RecyclingStationRequestDTO dto);
}
