package com.webdev.greenify.station.mapper;

import com.webdev.greenify.station.dto.AddressRequestDTO;
import com.webdev.greenify.station.dto.AddressResponseDTO;
import com.webdev.greenify.station.entity.AddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface AddressMapper {
    @Mapping(target = "recyclingStation", ignore = true)
    AddressEntity toAddressEntity(AddressRequestDTO dto);

    AddressResponseDTO toAddressResponseDTO(AddressEntity entity);

    @Mapping(target = "recyclingStation", ignore = true)
    void updateAddress(@MappingTarget AddressEntity entity, AddressRequestDTO dto);
}
