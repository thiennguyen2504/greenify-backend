package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.greenaction.dto.response.PointHistoryResponse;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PointMapper {

    PointHistoryResponse toPointHistoryResponse(PointTransactionEntity entity);
}
