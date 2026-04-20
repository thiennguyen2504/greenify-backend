package com.webdev.greenify.leaderboard.mapper;

import com.webdev.greenify.leaderboard.dto.response.PrizeConfigResponse;
import com.webdev.greenify.leaderboard.entity.LeaderboardPrizeConfigEntity;
import com.webdev.greenify.voucher.mapper.VoucherMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VoucherMapper.class})
public interface LeaderboardMapper {

    @Mapping(target = "nationalVoucher", source = "nationalVoucherTemplate")
    @Mapping(target = "provincialVoucher", source = "provincialVoucherTemplate")
    PrizeConfigResponse toPrizeConfigResponse(LeaderboardPrizeConfigEntity entity);
}
