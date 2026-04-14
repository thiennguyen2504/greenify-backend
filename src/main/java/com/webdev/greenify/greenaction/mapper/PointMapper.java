package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.greenaction.dto.response.PointHistoryResponse;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import org.mapstruct.Mapper;
adimport org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointMapper {

    @Mapping(target = "sourceName", ignore = true)
    @Mapping(target = "sourceDisplayUrl", ignore = true)
    PointHistoryResponse toPointHistoryResponse(PointTransactionEntity entity);
}
