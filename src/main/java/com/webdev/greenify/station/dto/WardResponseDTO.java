package com.webdev.greenify.station.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardResponseDTO {
    private Integer code;
    private String codename;
    private String divisionType;
    private String name;
    private Integer provinceCode;
}