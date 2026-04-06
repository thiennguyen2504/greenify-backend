package com.webdev.greenify.station.mapper;

import com.webdev.greenify.station.dto.WasteTypeResponseDTO;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import org.mapstruct.Mapper;

@Mapper
public interface WasteTypeMapper {
    WasteTypeResponseDTO toWasteTypeResponseDTO(WasteTypeEntity entity);
}
