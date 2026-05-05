package com.webdev.greenify.co2e.mapper;

import com.webdev.greenify.co2e.dto.response.Co2eTransactionResponse;
import com.webdev.greenify.co2e.dto.response.GreenImpactWalletResponse;
import com.webdev.greenify.co2e.entity.Co2eTransactionEntity;
import com.webdev.greenify.co2e.entity.GreenImpactWalletEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface Co2eMapper {

    @Mapping(target = "totalCo2eKg", expression = "java(sum(wallet.getTotalAvoidedKg(), wallet.getTotalAbsorbedKg()))")
    GreenImpactWalletResponse toWalletResponse(GreenImpactWalletEntity wallet);

    @Mapping(source = "createdAt", target = "creditedAt")
    Co2eTransactionResponse toTransactionResponse(Co2eTransactionEntity entity);

    default BigDecimal sum(BigDecimal left, BigDecimal right) {
        BigDecimal safeLeft = left != null ? left : BigDecimal.ZERO;
        BigDecimal safeRight = right != null ? right : BigDecimal.ZERO;
        return safeLeft.add(safeRight);
    }
}
