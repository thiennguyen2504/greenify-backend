package com.webdev.greenify.user.mapper;

import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.station.mapper.AddressMapper;
import com.webdev.greenify.user.dto.NGOPreviewDTO;
import com.webdev.greenify.user.dto.NGOProfileRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileResponseDTO;
import com.webdev.greenify.user.entity.NGOProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = { ImageMapper.class, AddressMapper.class })
public interface NGOProfileMapper {

    NGOProfileResponseDTO toDto(NGOProfileEntity entity);

    @Mapping(target = "name", source = "orgName")
    NGOPreviewDTO toPreviewDto(NGOProfileEntity entity);

    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "verificationDocs", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rejectedReason", ignore = true)
    @Mapping(target = "rejectedCount", ignore = true)
    NGOProfileEntity toEntity(NGOProfileRequestDTO dto);

    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "verificationDocs", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rejectedReason", ignore = true)
    @Mapping(target = "rejectedCount", ignore = true)
    void updateEntityFromDto(NGOProfileRequestDTO dto, @MappingTarget NGOProfileEntity entity);
}
