package com.webdev.greenify.station.mapper;

import com.webdev.greenify.station.dto.AddressRequestDTO;
import com.webdev.greenify.station.dto.AddressResponseDTO;
import com.webdev.greenify.station.dto.WasteTypeResponseDTO;
import com.webdev.greenify.station.entity.AddressEntity;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface WasteTypeMapper {
    WasteTypeResponseDTO toWasteTypeResponseDTO(WasteTypeEntity entity);
}
