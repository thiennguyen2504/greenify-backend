package com.webdev.greenify.voucher.mapper;

import com.webdev.greenify.voucher.dto.response.UserVoucherResponse;
import com.webdev.greenify.voucher.dto.response.VoucherTemplateResponse;
import com.webdev.greenify.voucher.entity.UserVoucherEntity;
import com.webdev.greenify.voucher.entity.VoucherTemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    VoucherTemplateResponse toVoucherTemplateResponse(VoucherTemplateEntity entity);

    @Mapping(target = "voucherTemplateId", source = "voucherTemplate.id")
    @Mapping(target = "voucherName", source = "voucherTemplate.name")
    @Mapping(target = "partnerName", source = "voucherTemplate.partnerName")
    @Mapping(target = "thumbnailUrl", source = "voucherTemplate.thumbnailUrl")
    UserVoucherResponse toUserVoucherResponse(UserVoucherEntity entity);
}