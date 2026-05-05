package com.webdev.greenify.co2e.constant;

import com.webdev.greenify.co2e.enumeration.Co2eType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum Co2eMaterialCode {

    // Nhóm rác tái chế
    PET_PLASTIC("Nhựa PET", Co2eType.AVOIDED, 2.0, 0.025, "chai"),
    HDPE_PLASTIC("Nhựa cứng HDPE", Co2eType.AVOIDED, 2.8, 0.05, "món"),
    PP_PLASTIC("Nhựa PP", Co2eType.AVOIDED, 2.8, 0.03, "món"),
    MIXED_PLASTIC("Nhựa hỗn hợp", Co2eType.AVOIDED, 2.3, 0.02, "món"),
    PAPER("Giấy", Co2eType.AVOIDED, 0.9, 0.004, "tờ"),
    CARDBOARD("Carton", Co2eType.AVOIDED, 1.0, 0.3, "thùng"),
    ALUMINUM_CAN("Lon nhôm", Co2eType.AVOIDED, 10.0, 0.015, "lon"),
    STEEL_CAN("Lon sắt/thép", Co2eType.AVOIDED, 1.5, 0.035, "lon"),
    METAL_MIXED("Kim loại hỗn hợp", Co2eType.AVOIDED, 2.0, null, "kg"),
    GLASS("Thủy tinh", Co2eType.AVOIDED, 0.3, 0.25, "chai"),
    MIXED_RECYCLABLES("Rác tái chế hỗn hợp", Co2eType.AVOIDED, 0.8, null, "kg"),

    // Nhóm hữu cơ / dọn rác
    ORGANIC_COMPOST("Ủ rác hữu cơ", Co2eType.AVOIDED, 0.6, 0.2, "phần"),
    CLEANUP_MIXED_WASTE("Dọn rác hỗn hợp", Co2eType.AVOIDED, 0.4, 1.0, "túi"),
    E_WASTE_SMALL("Rác điện tử nhỏ", Co2eType.AVOIDED, 1.0, 0.1, "món"),
    HAZARDOUS_WASTE("Rác nguy hại", Co2eType.AVOIDED, 0.0, null, "món"),

    // Nhóm tái sử dụng
    REUSABLE_BOTTLE("Bình nước cá nhân", Co2eType.AVOIDED, 0.03, null, "lần"),
    REUSABLE_BAG("Túi vải tái sử dụng", Co2eType.AVOIDED, 0.02, null, "lần"),
    REUSABLE_BOX("Hộp cá nhân", Co2eType.AVOIDED, 0.05, null, "lần"),
    DONATE_TEXTILE("Quyên góp quần áo", Co2eType.AVOIDED, 0.5, null, "món"),
    TEXTILE_RECYCLE("Tái chế vải", Co2eType.AVOIDED, 2.0, null, "kg"),

    // Nhóm trồng cây
    TREE_PLANTING_PENDING("Mới trồng (chờ xác thực)", Co2eType.ABSORBED, 0.0, null, "cây"),
    TREE_PLANTING_VERIFIED("Cây trồng đã xác thực", Co2eType.ABSORBED, 5.0, null, "cây"),
    TREE_SURVIVED_6M("Cây sống sau 6 tháng", Co2eType.ABSORBED, 2.5, null, "cây"),
    TREE_SURVIVED_1Y("Cây sống sau 1 năm", Co2eType.ABSORBED, 5.0, null, "cây"),
    TREE_EVENT_NGO("Trồng cây sự kiện NGO", Co2eType.ABSORBED, 7.5, null, "cây"),

    UNKNOWN("Không xác định", Co2eType.AVOIDED, 0.0, null, null);

    private final String label;
    private final Co2eType co2eType;
    private final double co2eFactorKgPerUnit;
    private final Double defaultWeightKgPerItem;
    private final String unit;

    Co2eMaterialCode(String label, Co2eType co2eType, double co2eFactorKgPerUnit, Double defaultWeightKgPerItem, String unit) {
        this.label = label;
        this.co2eType = co2eType;
        this.co2eFactorKgPerUnit = co2eFactorKgPerUnit;
        this.defaultWeightKgPerItem = defaultWeightKgPerItem;
        this.unit = unit;
    }

    public boolean isWeightBased() {
        return defaultWeightKgPerItem != null;
    }

    public boolean isReusableAction() {
        return this == REUSABLE_BOTTLE || this == REUSABLE_BAG || this == REUSABLE_BOX;
    }

    public boolean isTreePlanting() {
        return co2eType == Co2eType.ABSORBED;
    }

    public boolean shouldCreditCo2e() {
        return co2eFactorKgPerUnit > 0;
    }
}
