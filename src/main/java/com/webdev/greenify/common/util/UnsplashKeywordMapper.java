package com.webdev.greenify.common.util;

import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class UnsplashKeywordMapper {

    public static final Map<String, String> ACTION_GROUP_KEYWORDS = Map.ofEntries(
            Map.entry("Waste Sorting", "recycling,sorting,waste"),
            Map.entry("Recycling", "recycling,plastic,bottle"),
            Map.entry("Plastic Reduction", "reusable,bag,bottle,eco"),
            Map.entry("Resource Conservation", "electricity,water,conservation"),
            Map.entry("Green Transportation", "bicycle,walking,bus,public-transport"),
            Map.entry("Environmental Cleanup", "cleanup,volunteer,trash,beach"),
            Map.entry("Greenery", "planting,tree,garden,green"),
            Map.entry("Reuse", "upcycle,reuse,handmade"),
            Map.entry("Green Consumption", "organic,eco-product,sustainable"),
            Map.entry("Community Participation", "volunteer,community,event"),
            Map.entry("Phân loại rác", "recycling,sorting,waste"),
            Map.entry("Tái chế", "recycling,plastic,bottle"),
            Map.entry("Giảm nhựa dùng một lần", "reusable,bag,bottle,eco"),
            Map.entry("Tiết kiệm tài nguyên", "electricity,water,conservation"),
            Map.entry("Di chuyển xanh", "bicycle,walking,bus,public-transport"),
            Map.entry("Dọn dẹp môi trường", "cleanup,volunteer,trash,beach"),
            Map.entry("Mảng xanh", "planting,tree,garden,green"),
            Map.entry("Tái sử dụng", "upcycle,reuse,handmade"),
            Map.entry("Tiêu dùng xanh", "organic,eco-product,sustainable"),
            Map.entry("Tham gia cộng đồng", "volunteer,community,event")
    );

    public static final Map<String, String> WASTE_TYPE_KEYWORDS = Map.ofEntries(
            Map.entry("Plastic", "plastic-waste,pollution"),
            Map.entry("Paper", "paper-waste,cardboard"),
            Map.entry("Metal", "metal-scrap,recycling"),
            Map.entry("Glass", "glass-bottle,recycling"),
            Map.entry("Organic", "compost,organic-waste"),
            Map.entry("Hazardous", "chemical-waste,industrial"),
            Map.entry("Nhựa", "plastic-waste,pollution"),
            Map.entry("Giấy", "paper-waste,cardboard"),
            Map.entry("Kim loại", "metal-scrap,recycling"),
            Map.entry("Thủy tinh", "glass-bottle,recycling"),
            Map.entry("Hữu cơ", "compost,organic-waste"),
            Map.entry("Nguy hại", "chemical-waste,industrial")
    );

    public static final Map<String, String> PLANT_KEYWORDS = Map.ofEntries(
            Map.entry("Hướng dương", "sunflower"),
            Map.entry("Hoa hồng", "rose,flower"),
            Map.entry("Cẩm chướng", "carnation,flower"),
            Map.entry("Sen", "lotus"),
            Map.entry("Anh đào", "cherry-blossom"),
            Map.entry("Cây phong", "maple-tree"),
            Map.entry("Cây thông", "pine-tree"),
            Map.entry("Cây dừa", "coconut-palm"),
            Map.entry("Hoa tulip", "tulip"),
            Map.entry("Hoa mai", "yellow-flower"),
            Map.entry("Hoa lan", "orchid"),
            Map.entry("Cây táo", "apple-tree"),
            Map.entry("Tre", "bamboo"),
            Map.entry("Xương rồng nở hoa", "cactus-flower")
    );

    public static final Map<String, String> EVENT_KEYWORDS = Map.ofEntries(
            Map.entry("CLEANUP", "beach-cleanup,volunteer,environment"),
            Map.entry("PLANTING", "tree-planting,reforestation,green"),
            Map.entry("RECYCLING", "recycling,community,eco"),
            Map.entry("EDUCATION", "education,workshop,environment"),
            Map.entry("OTHER", "volunteer,green,nature")
    );

    public static String getActionKeyword(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return "environment,green";
        }
        return ACTION_GROUP_KEYWORDS.getOrDefault(groupName, "environment,green");
    }

    public static String getPlantKeyword(String seedName) {
        if (seedName == null || seedName.isBlank()) {
            return "plant,green";
        }
        return PLANT_KEYWORDS.getOrDefault(seedName, "plant,green");
    }

    public static String getEventKeyword(String eventTypeName) {
        if (eventTypeName == null || eventTypeName.isBlank()) {
            return "volunteer,green,nature";
        }
        return EVENT_KEYWORDS.getOrDefault(eventTypeName, "volunteer,green,nature");
    }

    public static String getTrashSpotKeyword(String province, String wasteTypeName) {
        String baseKeyword = WASTE_TYPE_KEYWORDS.getOrDefault(wasteTypeName, "waste,pollution");
        if (province != null && province.contains("Hồ Chí Minh")) {
            return baseKeyword + ",vietnam,city";
        }
        return baseKeyword + ",vietnam";
    }
}
