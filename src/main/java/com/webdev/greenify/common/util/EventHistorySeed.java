package com.webdev.greenify.common.util;

import com.webdev.greenify.file.entity.EventImageEntity;
import com.webdev.greenify.file.enumeration.EventImageType;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.station.entity.AddressEntity;
import com.webdev.greenify.station.service.ProvinceNormalizationService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventHistorySeed {

    private static final long COMPLETED_EVENT_THRESHOLD = 10;

    private static final Map<String, double[]> PROVINCE_COORDINATES = Map.of(
            "Thành phố Hồ Chí Minh", new double[]{10.7756, 106.7019},
            "Hà Nội", new double[]{21.0285, 105.8542},
            "Đà Nẵng", new double[]{16.0544, 108.2022},
            "Hải Phòng", new double[]{20.8449, 106.6881},
            "Cần Thơ", new double[]{10.0452, 105.7469},
            "Lâm Đồng", new double[]{11.9464, 108.4419}
    );

    private static final Map<String, String> PROVINCE_SHORT_NAMES = Map.of(
            "Thành phố Hồ Chí Minh", "TP.HCM",
            "Hà Nội", "Hà Nội",
            "Đà Nẵng", "Đà Nẵng",
            "Hải Phòng", "Hải Phòng",
            "Cần Thơ", "Cần Thơ",
            "Lâm Đồng", "Lâm Đồng"
    );

    private static final Map<String, String> DISTRICT_BY_PROVINCE = Map.of(
            "Thành phố Hồ Chí Minh", "Quận 1",
            "Hà Nội", "Quận Hoàn Kiếm",
            "Đà Nẵng", "Quận Hải Châu",
            "Hải Phòng", "Quận Ngô Quyền",
            "Cần Thơ", "Quận Ninh Kiều",
            "Lâm Đồng", "TP. Đà Lạt"
    );

    private static final Map<GreenEventType, List<String>> TITLE_POOL = Map.of(
            GreenEventType.CLEANUP, List.of(
                    "Dọn dẹp bãi biển %s",
                    "Chiến dịch làm sạch kênh rạch %s",
                    "Ngày hội dọn rác %s"),
            GreenEventType.PLANTING, List.of(
                    "Trồng cây gây rừng tại %s",
                    "Phủ xanh đô thị %s",
                    "Ngày hội trồng cây %s"),
            GreenEventType.RECYCLING, List.of(
                    "Ngày hội tái chế %s",
                    "Đổi rác lấy quà tại %s",
                    "Chiến dịch tái chế %s"),
            GreenEventType.EDUCATION, List.of(
                    "Workshop bảo vệ môi trường %s",
                    "Giáo dục xanh tại %s",
                    "Hội thảo sống xanh %s"),
            GreenEventType.OTHER, List.of(
                    "Ngày hội xanh %s",
                    "Sự kiện cộng đồng xanh %s")
    );

    private static final Map<GreenEventType, String> DESCRIPTION_BY_TYPE = Map.of(
            GreenEventType.CLEANUP,
            "Hoạt động cộng đồng nhằm thu gom rác và làm sạch môi trường sống.",
            GreenEventType.PLANTING,
            "Sự kiện trồng cây góp phần cải thiện không gian xanh đô thị.",
            GreenEventType.RECYCLING,
            "Chương trình thúc đẩy phân loại và tái chế rác thải đúng cách.",
            GreenEventType.EDUCATION,
            "Buổi chia sẻ kiến thức và kỹ năng sống xanh cho cộng đồng.",
            GreenEventType.OTHER,
            "Hoạt động xanh kết nối cộng đồng vì môi trường bền vững."
    );

    private static final List<String> ADDRESS_DETAILS = List.of(
            "12 Lê Lợi",
            "24 Nguyễn Huệ",
            "36 Trần Hưng Đạo",
            "58 Phan Đình Phùng",
            "72 Hai Bà Trưng",
            "96 Võ Văn Tần",
            "118 Điện Biên Phủ",
            "140 Lý Tự Trọng"
    );

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final UnsplashImageService unsplashImageService;
    private final ProvinceNormalizationService provinceNormalizationService;
    private final Random random = new Random();

    @Transactional
    public void seed() {
        normalizeAllLegacyEvents();
        if (eventRepository.countCompletedEvents() > COMPLETED_EVENT_THRESHOLD) {
            log.info("Skip EventHistorySeed - already seeded");
            return;
        }

        try {
            List<UserEntity> ngos = userRepository.findAll().stream()
                    .filter(user -> user.getRoles() != null
                            && user.getRoles().stream().anyMatch(role -> "NGO".equalsIgnoreCase(role.getName())))
                    .toList();

            if (ngos.isEmpty()) {
                log.warn("Skip EventHistorySeed because no NGO users found");
                return;
            }

            List<EventTemplate> templates = buildTemplates();
            List<EventEntity> events = new ArrayList<>();

            for (int i = 0; i < templates.size(); i++) {
                EventTemplate template = templates.get(i);
                UserEntity organizer = ngos.get(i % ngos.size());
                events.add(buildCompletedEvent(template, organizer, ngos));
            }

            eventRepository.saveAll(events);
            log.info("EventHistorySeed completed (events={})", events.size());
        } catch (Exception e) {
            log.warn("EventHistorySeed failed: {}", e.getMessage(), e);
        }
    }

    private EventEntity buildCompletedEvent(EventTemplate template, UserEntity organizer, List<UserEntity> ngos) {
        LocalDateTime startTime = LocalDateTime.now()
                .minusDays(template.daysAgo())
                .withHour(template.startHour())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime endTime = startTime.withHour(template.endHour());

        String normalizedProvince = normalizeProvince(template.province());

        AddressEntity address = AddressEntity.builder()
                .province(normalizedProvince)
                .district(resolveDistrict(normalizedProvince))
                .ward("Phường " + (random.nextInt(15) + 1))
                .addressDetail(randomAddressDetail())
                .latitude(resolveLatitude(normalizedProvince))
                .longitude(resolveLongitude(normalizedProvince))
                .build();

        String thumbUrl = resolveImageUrl(template.type());

        EventImageEntity thumbnail = EventImageEntity.builder()
                .bucketName("cdn.greenify.io.vn")
                .objectKey("images/event_" + UUID.randomUUID().toString().substring(0, 8))
                .imageUrl(thumbUrl)
                .imageType(EventImageType.THUMBNAIL)
                .build();

        EventEntity event = EventEntity.builder()
                .title(buildTitle(template.type(), normalizedProvince))
                .description(buildDescription(template.type()))
                .eventType(template.type())
                .startTime(startTime)
                .endTime(endTime)
                .maxParticipants(template.participantCount() + 30)
                .minParticipants(10L)
                .signUpDeadlineHoursBefore(24L)
                .cancelDeadlineHoursBefore(48L)
                .rewardPoints(100.0)
                .status(GreenEventStatus.COMPLETED)
                .participantCount(template.participantCount())
                .organizer(organizer)
                .address(address)
                .build();

        thumbnail.setEvent(event);
        event.getImages().add(thumbnail);
        return event;
    }

    private String resolveImageUrl(GreenEventType eventType) {
        String fallbackUrl = "https://picsum.photos/seed/" + eventType.name().toLowerCase(Locale.ROOT) + "/800/600";
        String keyword = UnsplashKeywordMapper.getEventKeyword(eventType.name());

        try {
            return unsplashImageService.isEnabled()
                    ? unsplashImageService.getImageUrl(keyword)
                    : fallbackUrl;
        } catch (Exception e) {
            return fallbackUrl;
        }
    }

    private String buildTitle(GreenEventType eventType, String province) {
        List<String> titleTemplates = TITLE_POOL.getOrDefault(eventType, TITLE_POOL.get(GreenEventType.OTHER));
        String selectedTemplate = titleTemplates.get(random.nextInt(titleTemplates.size()));
        String shortName = PROVINCE_SHORT_NAMES.getOrDefault(province, province);
        return String.format(selectedTemplate, shortName);
    }

    private String buildDescription(GreenEventType eventType) {
        return DESCRIPTION_BY_TYPE.getOrDefault(eventType, DESCRIPTION_BY_TYPE.get(GreenEventType.OTHER));
    }

    private String resolveDistrict(String province) {
        return DISTRICT_BY_PROVINCE.getOrDefault(province, "Quận Trung tâm");
    }

    private String randomAddressDetail() {
        return ADDRESS_DETAILS.get(random.nextInt(ADDRESS_DETAILS.size()));
    }

    private BigDecimal resolveLatitude(String province) {
        double[] coordinates = PROVINCE_COORDINATES.getOrDefault(province, new double[]{10.7756, 106.7019});
        return BigDecimal.valueOf(coordinates[0]);
    }

    private BigDecimal resolveLongitude(String province) {
        double[] coordinates = PROVINCE_COORDINATES.getOrDefault(province, new double[]{10.7756, 106.7019});
        return BigDecimal.valueOf(coordinates[1]);
    }

    private String normalizeProvince(String province) {
        String normalized = provinceNormalizationService.normalizeProvinceName(province);
        return normalized == null || normalized.isBlank() ? province : normalized;
    }

    @Transactional
    public void normalizeAllLegacyEvents() {
        log.info("Bắt đầu chuẩn hóa dữ liệu event cũ...");
        
        // Lấy danh sách các event có tên tỉnh kiểu cũ
        List<EventEntity> legacyEvents = eventRepository.findAll(); 
        int updatedCount = 0;

        for (EventEntity event : legacyEvents) {
            if (event.getAddress() != null && event.getAddress().getProvince() != null) {
                String currentProvince = event.getAddress().getProvince();
                String normalized = provinceNormalizationService.normalizeProvinceName(currentProvince);
                
                // Nếu tên sau khi chuẩn hóa khác tên cũ, tiến hành cập nhật
                if (normalized != null && !normalized.equals(currentProvince)) {
                    event.getAddress().setProvince(normalized);
                    updatedCount++;
                }
            }
        }
        
        if (updatedCount > 0) {
            eventRepository.saveAll(legacyEvents);
            log.info("Đã cập nhật chuẩn hóa cho {} sự kiện.", updatedCount);
        } else {
            log.info("Không tìm thấy dữ liệu cần chuẩn hóa.");
        }
    }

    private List<EventTemplate> buildTemplates() {
        return List.of(
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 7, 11, 85L, 180),
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 7, 11, 120L, 150),
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 8, 12, 95L, 130),
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 8, 12, 110L, 100),
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 14, 17, 67L, 90),
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 14, 17, 78L, 70),
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 7, 11, 130L, 60),
                new EventTemplate(GreenEventType.CLEANUP, "Thành phố Hồ Chí Minh", 8, 12, 88L, 30),

                new EventTemplate(GreenEventType.CLEANUP, "Hà Nội", 7, 11, 72L, 170),
                new EventTemplate(GreenEventType.CLEANUP, "Hà Nội", 8, 12, 91L, 120),
                new EventTemplate(GreenEventType.CLEANUP, "Hà Nội", 14, 17, 55L, 80),
                new EventTemplate(GreenEventType.CLEANUP, "Hà Nội", 7, 11, 100L, 45),

                new EventTemplate(GreenEventType.PLANTING, "Thành phố Hồ Chí Minh", 6, 10, 65L, 160),
                new EventTemplate(GreenEventType.PLANTING, "Thành phố Hồ Chí Minh", 7, 11, 80L, 140),
                new EventTemplate(GreenEventType.PLANTING, "Thành phố Hồ Chí Minh", 6, 10, 72L, 110),
                new EventTemplate(GreenEventType.PLANTING, "Thành phố Hồ Chí Minh", 7, 11, 55L, 75),
                new EventTemplate(GreenEventType.PLANTING, "Thành phố Hồ Chí Minh", 14, 16, 43L, 50),
                new EventTemplate(GreenEventType.PLANTING, "Thành phố Hồ Chí Minh", 7, 11, 90L, 20),

                new EventTemplate(GreenEventType.PLANTING, "Hà Nội", 6, 10, 48L, 155),
                new EventTemplate(GreenEventType.PLANTING, "Hà Nội", 7, 11, 63L, 95),
                new EventTemplate(GreenEventType.PLANTING, "Hà Nội", 14, 16, 35L, 40),

                new EventTemplate(GreenEventType.RECYCLING, "Thành phố Hồ Chí Minh", 8, 16, 150L, 175),
                new EventTemplate(GreenEventType.RECYCLING, "Thành phố Hồ Chí Minh", 8, 16, 180L, 135),
                new EventTemplate(GreenEventType.RECYCLING, "Thành phố Hồ Chí Minh", 9, 17, 130L, 100),
                new EventTemplate(GreenEventType.RECYCLING, "Thành phố Hồ Chí Minh", 8, 16, 200L, 55),
                new EventTemplate(GreenEventType.RECYCLING, "Thành phố Hồ Chí Minh", 9, 17, 165L, 25),

                new EventTemplate(GreenEventType.RECYCLING, "Hà Nội", 8, 16, 120L, 165),
                new EventTemplate(GreenEventType.RECYCLING, "Hà Nội", 8, 16, 140L, 110),
                new EventTemplate(GreenEventType.RECYCLING, "Hà Nội", 9, 17, 98L, 50),

                new EventTemplate(GreenEventType.EDUCATION, "Đà Nẵng", 8, 11, 85L, 170),
                new EventTemplate(GreenEventType.EDUCATION, "Đà Nẵng", 9, 12, 70L, 120),
                new EventTemplate(GreenEventType.EDUCATION, "Đà Nẵng", 14, 17, 92L, 80),
                new EventTemplate(GreenEventType.EDUCATION, "Đà Nẵng", 8, 11, 78L, 35),

                new EventTemplate(GreenEventType.EDUCATION, "Thành phố Hồ Chí Minh", 8, 11, 110L, 145),
                new EventTemplate(GreenEventType.EDUCATION, "Thành phố Hồ Chí Minh", 9, 12, 125L, 100),
                new EventTemplate(GreenEventType.EDUCATION, "Thành phố Hồ Chí Minh", 14, 17, 95L, 65),
                new EventTemplate(GreenEventType.EDUCATION, "Thành phố Hồ Chí Minh", 8, 11, 140L, 28),

                new EventTemplate(GreenEventType.OTHER, "Hải Phòng", 7, 11, 60L, 160),
                new EventTemplate(GreenEventType.OTHER, "Cần Thơ", 8, 12, 45L, 100),
                new EventTemplate(GreenEventType.OTHER, "Lâm Đồng", 7, 11, 38L, 60)
        );
    }

    private record EventTemplate(
            GreenEventType type,
            String province,
            int startHour,
            int endHour,
            long participantCount,
            int daysAgo) {
    }
}