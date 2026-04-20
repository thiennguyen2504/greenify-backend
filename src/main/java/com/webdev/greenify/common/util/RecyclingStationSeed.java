package com.webdev.greenify.common.util;

import com.webdev.greenify.station.entity.AddressEntity;
import com.webdev.greenify.station.entity.OpenTimeEntity;
import com.webdev.greenify.station.entity.RecyclingStationEntity;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.station.enumeration.StationStatus;
import com.webdev.greenify.station.repository.RecyclingStationRepository;
import com.webdev.greenify.station.repository.WasteTypeRepository;
import com.webdev.greenify.station.service.ProvinceNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecyclingStationSeed {

    private static final long SEED_THRESHOLD = 3;

    private final RecyclingStationRepository recyclingStationRepository;
    private final WasteTypeRepository wasteTypeRepository;
    private final ProvinceNormalizationService provinceNormalizationService;

    @Transactional
    public void seed() {
        if (recyclingStationRepository.count() > SEED_THRESHOLD) {
            log.info("Skip RecyclingStationSeed - already seeded");
            return;
        }

        try {
            Set<String> existingNames = new HashSet<>(
                    recyclingStationRepository.findAll().stream().map(RecyclingStationEntity::getName).toList());

            int seededCount = 0;
            List<StationTemplate> templates = buildStationTemplates();
            for (int index = 0; index < templates.size(); index++) {
                StationTemplate template = templates.get(index);
                if (existingNames.contains(template.name())) {
                    continue;
                }

                List<WasteTypeEntity> wasteTypes = resolveWasteTypes(template.wasteTypeNames());
                if (wasteTypes.isEmpty()) {
                    log.warn("Skip recycling station {} because waste types are missing", template.name());
                    continue;
                }

                RecyclingStationEntity station = RecyclingStationEntity.builder()
                        .name(template.name())
                        .description("Điểm thu gom và phân loại rác tái chế: " + String.join(", ", template.wasteTypeNames()))
                        .phoneNumber(String.format("09000000%02d", index + 1))
                        .email("station" + (index + 1) + "@greenify.vn")
                        .status(StationStatus.ACTIVE)
                        .build();

                AddressEntity address = AddressEntity.builder()
                        .province(normalizeProvince(template.province()))
                        .district(template.district())
                        .ward(template.ward())
                        .addressDetail(template.addressDetail())
                        .latitude(BigDecimal.valueOf(template.latitude()))
                        .longitude(BigDecimal.valueOf(template.longitude()))
                        .build();
                station.setAddress(address);
                station.setWasteTypes(wasteTypes);

                List<OpenTimeEntity> openTimes = new ArrayList<>();
                for (OpenTimeTemplate openTimeTemplate : template.openTimes()) {
                    openTimes.add(OpenTimeEntity.builder()
                            .startTime(LocalTime.of(openTimeTemplate.startHour(), openTimeTemplate.startMinute()))
                            .endTime(LocalTime.of(openTimeTemplate.endHour(), openTimeTemplate.endMinute()))
                            .dayOfWeek(openTimeTemplate.dayOfWeek())
                            .recyclingStation(station)
                            .build());
                }
                station.setOpenTimes(openTimes);

                recyclingStationRepository.save(station);
                seededCount++;
            }

            log.info("RecyclingStationSeed completed (seeded={})", seededCount);
        } catch (Exception e) {
            log.warn("RecyclingStationSeed failed: {}", e.getMessage(), e);
        }
    }

    private List<WasteTypeEntity> resolveWasteTypes(List<String> wasteTypeNames) {
        List<WasteTypeEntity> wasteTypes = new ArrayList<>();
        for (String wasteTypeName : wasteTypeNames) {
            WasteTypeEntity wasteType = wasteTypeRepository.findByName(wasteTypeName).orElse(null);
            if (wasteType == null) {
                log.warn("Waste type not found: {}", wasteTypeName);
                continue;
            }
            wasteTypes.add(wasteType);
        }
        return wasteTypes;
    }

    private String normalizeProvince(String province) {
        return provinceNormalizationService.normalizeProvinceName(province);
    }

    private List<StationTemplate> buildStationTemplates() {
        List<StationTemplate> templates = new ArrayList<>();

        List<OpenTimeTemplate> station1OpenTimes = new ArrayList<>(openTimesForRange(
                DayOfWeek.MONDAY,
                DayOfWeek.FRIDAY,
                7,
                0,
                18,
                0));
        station1OpenTimes.add(new OpenTimeTemplate(DayOfWeek.SATURDAY, 8, 0, 12, 0));
        templates.add(new StationTemplate(
                "Điểm Thu Gom Tái Chế Quận 1",
                "Thành phố Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "45 Lý Tự Trọng",
                10.7756,
                106.7019,
                List.of("Nhựa", "Giấy", "Kim loại"),
                station1OpenTimes));

        templates.add(new StationTemplate(
                "Trạm Tái Chế Bình Thạnh",
                "Thành phố Hồ Chí Minh",
                "Quận Bình Thạnh",
                "Phường 26",
                "210 Đinh Bộ Lĩnh",
                10.8121,
                106.7124,
                List.of("Nhựa", "Thủy tinh", "Hữu cơ"),
                openTimesForRange(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, 6, 0, 20, 0)));

        templates.add(new StationTemplate(
                "Điểm Tập Kết Rác Tái Chế Thủ Đức",
                "Thành phố Hồ Chí Minh",
                "TP. Thủ Đức",
                "Phường Linh Chiểu",
                "15 Võ Văn Ngân",
                10.8631,
                106.7764,
                List.of("Nhựa", "Giấy", "Kim loại", "Nguy hại"),
                openTimesForRange(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, 7, 30, 17, 30)));

        templates.add(new StationTemplate(
                "Trạm Thu Hồi Rác Điện Tử Cầu Giấy",
                "Hà Nội",
                "Quận Cầu Giấy",
                "Phường Dịch Vọng Hậu",
                "78 Trần Thái Tông",
                21.0312,
                105.7985,
                List.of("Nguy hại", "Kim loại"),
                openTimesForRange(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, 8, 0, 17, 0)));

        templates.add(new StationTemplate(
                "Điểm Tái Chế Xanh Hoàn Kiếm",
                "Hà Nội",
                "Quận Hoàn Kiếm",
                "Phường Hàng Bài",
                "12 Hàng Bài",
                21.0283,
                105.8513,
                List.of("Giấy", "Nhựa", "Thủy tinh"),
                openTimesForRange(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, 7, 0, 19, 0)));

        templates.add(new StationTemplate(
                "Trạm Phân Loại Rác Đống Đa",
                "Hà Nội",
                "Quận Đống Đa",
                "Phường Hàng Bột",
                "55 Tôn Đức Thắng",
                21.0245,
                105.8398,
                List.of("Hữu cơ", "Nhựa", "Giấy"),
                openTimesForRange(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, 6, 30, 18, 30)));

        templates.add(new StationTemplate(
                "Trung Tâm Tái Chế Hải Châu",
                "Đà Nẵng",
                "Quận Hải Châu",
                "Phường Nam Dương",
                "98 Nguyễn Văn Linh",
                16.0624,
                108.2192,
                List.of("Nhựa", "Kim loại", "Thủy tinh"),
                openTimesForRange(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, 7, 0, 17, 0)));

        templates.add(new StationTemplate(
                "Điểm Thu Gom Pin Cũ Ninh Kiều",
                "Cần Thơ",
                "Quận Ninh Kiều",
                "Phường Tân An",
                "34 Phan Đình Phùng",
                10.0347,
                105.7878,
                List.of("Nguy hại"),
                openTimesForRange(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, 8, 0, 16, 0)));

        return templates;
    }

    private List<OpenTimeTemplate> openTimesForRange(
            DayOfWeek from,
            DayOfWeek to,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute) {

        List<OpenTimeTemplate> openTimes = new ArrayList<>();
        for (int dayValue = from.getValue(); dayValue <= to.getValue(); dayValue++) {
            openTimes.add(new OpenTimeTemplate(
                    DayOfWeek.of(dayValue),
                    startHour,
                    startMinute,
                    endHour,
                    endMinute));
        }
        return openTimes;
    }

    private record StationTemplate(
            String name,
            String province,
            String district,
            String ward,
            String addressDetail,
            double latitude,
            double longitude,
            List<String> wasteTypeNames,
            List<OpenTimeTemplate> openTimes) {
    }

    private record OpenTimeTemplate(
            DayOfWeek dayOfWeek,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute) {
    }
}