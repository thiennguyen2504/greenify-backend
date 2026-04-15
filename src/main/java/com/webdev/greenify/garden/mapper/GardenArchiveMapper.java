package com.webdev.greenify.garden.mapper;

import com.webdev.greenify.garden.dto.response.GardenArchiveResponse;
import com.webdev.greenify.garden.entity.GardenArchiveEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GardenArchiveMapper {

    @Mapping(target = "seedName", source = "seed.name")
    @Mapping(target = "voucherCode", source = "userVoucher.voucherCode")
    GardenArchiveResponse toGardenArchiveResponse(GardenArchiveEntity entity);
}
