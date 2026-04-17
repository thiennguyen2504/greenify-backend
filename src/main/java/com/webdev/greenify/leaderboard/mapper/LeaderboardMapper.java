package com.webdev.greenify.leaderboard.mapper;

import com.webdev.greenify.leaderboard.dto.response.PrizeConfigResponse;
import com.webdev.greenify.leaderboard.entity.LeaderboardPrizeConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaderboardMapper {

    @Mapping(target = "nationalVoucherTemplateId", source = "nationalVoucherTemplate.id")
    @Mapping(target = "provincialVoucherTemplateId", source = "provincialVoucherTemplate.id")
    PrizeConfigResponse toPrizeConfigResponse(LeaderboardPrizeConfigEntity entity);
}
