package com.webdev.greenify.garden.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlantationBuilding {
    GARAGE_A("Nhà xe A"),
    GARAGE_B("Nhà xe B"),
    CAFETERIA_A("Cantin A"),
    CAFETERIA_B("Cantin B"),
    BUILDING_A("Tòa A"),
    BUILDING_B("Tòa B"),
    BUILDING_C("Tòa C"),
    BUILDING_E("Tòa E");

    private final String displayName;
}
