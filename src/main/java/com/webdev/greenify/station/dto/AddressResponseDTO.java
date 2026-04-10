package com.webdev.greenify.station.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponseDTO {
    private String id;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
