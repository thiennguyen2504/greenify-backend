package com.webdev.greenify.common.util;

import com.webdev.greenify.file.entity.EventImageEntity;
import com.webdev.greenify.file.enumeration.EventImageType;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.station.entity.AddressEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.enumeration.ImageStatus;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventSeed {

    private static final long SEED_THRESHOLD = 3;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final UnsplashImageService unsplashImageService;

    private final AtomicInteger poolCursor = new AtomicInteger(0);

    @Transactional
    public void seed() {
        if (eventRepository.count() > SEED_THRESHOLD) {
            log.info("Skip EventSeed because event count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            UserEntity organizer = findOrganizerUser();
            if (organizer == null) {
                log.warn("Skip EventSeed because no organizer user found");
                return;
            }

            List<ImageData> imagePool = fetchImagePool(12);
            List<EventPlan> plans = buildEventPlans();

            for (EventPlan plan : plans) {
                try {
                    EventEntity event = buildEventEntity(plan, organizer, imagePool);
                    eventRepository.save(event);
                    log.info("Seeded event: {} ({})", event.getTitle(), event.getId());
                } catch (Exception ex) {
                    log.warn("Skip seeding event {} due to error: {}", plan.title(), ex.getMessage());
                }
            }

            log.info("EventSeed completed with {} planned events", plans.size());
        } catch (Exception ex) {
            log.warn("EventSeed failed: {}", ex.getMessage(), ex);
        }
    }

    private EventEntity buildEventEntity(EventPlan plan, UserEntity organizer, List<ImageData> imagePool) {
        LocalDateTime startTime = LocalDateTime.now().plusDays(plan.startInDays()).withHour(7).withMinute(30).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(4);

        AddressEntity address = AddressEntity.builder()
                .province(plan.province())
                .district(plan.district())
                .ward(plan.ward())
                .addressDetail(plan.addressDetail())
            .latitude(BigDecimal.valueOf(plan.latitude()))
            .longitude(BigDecimal.valueOf(plan.longitude()))
                .build();

        EventEntity event = EventEntity.builder()
                .title(plan.title())
                .description(plan.description())
                .eventType(plan.eventType())
                .startTime(startTime)
                .endTime(endTime)
                .maxParticipants(plan.maxParticipants())
                .minParticipants(plan.minParticipants())
                .cancelDeadlineHoursBefore(24L)
                .signUpDeadlineHoursBefore(12L)
                .reminderHoursBefore(2L)
                .thankYouHoursAfter(2L)
                .rewardPoints(plan.rewardPoints())
                .status(GreenEventStatus.PUBLISHED)
                .rejectedCount(0)
                .participantCount(0L)
                .participationConditions("Mang theo bình nước cá nhân và tuân thủ hướng dẫn an toàn môi trường.")
                .organizer(organizer)
                .address(address)
                .build();

        String keyword = UnsplashKeywordMapper.getEventKeyword(plan.eventType().name());
        String thumbnailUrl = resolveThumbnailUrl(keyword, imagePool);
        String detailUrl = resolveDetailUrl(keyword, imagePool);

        EventImageEntity thumbnailImage = EventImageEntity.builder()
                .event(event)
                .bucketName("unsplash-cdn")
                .objectKey("unsplash/event/thumbnail/" + UUID.randomUUID())
                .imageUrl(thumbnailUrl)
                .status(ImageStatus.ACTIVE)
                .imageType(EventImageType.THUMBNAIL)
                .build();

        EventImageEntity detailImage = EventImageEntity.builder()
                .event(event)
                .bucketName("unsplash-cdn")
                .objectKey("unsplash/event/detail/" + UUID.randomUUID())
                .imageUrl(detailUrl)
                .status(ImageStatus.ACTIVE)
                .imageType(EventImageType.DETAIL)
                .build();

        event.getImages().add(thumbnailImage);
        event.getImages().add(detailImage);

        return event;
    }

    private String resolveThumbnailUrl(String keyword, List<ImageData> imagePool) {
        if (unsplashImageService.isEnabled()) {
            return unsplashImageService.getImageUrl(keyword + ",outdoor");
        }
        return nextImageFromPool(imagePool).imageUrl();
    }

    private String resolveDetailUrl(String keyword, List<ImageData> imagePool) {
        if (unsplashImageService.isEnabled()) {
            return unsplashImageService.getImageUrl(keyword + ",nature,people");
        }
        return nextImageFromPool(imagePool).imageUrl();
    }

    private ImageData nextImageFromPool(List<ImageData> imagePool) {
        if (imagePool == null || imagePool.isEmpty()) {
            return new ImageData(
                    "seed-fallback",
                    "fallback/event/" + UUID.randomUUID(),
                    "https://picsum.photos/seed/event-fallback/1200/800");
        }

        int index = Math.floorMod(poolCursor.getAndIncrement(), imagePool.size());
        return imagePool.get(index);
    }

    private List<ImageData> fetchImagePool(int count) {
        if (unsplashImageService.isEnabled()) {
            try {
                List<String> urls = unsplashImageService.getMultipleImageUrls("volunteer,environment,vietnam", count);
                List<ImageData> dynamicPool = new ArrayList<>();
                for (String url : urls) {
                    dynamicPool.add(new ImageData(
                            "unsplash-cdn",
                            "unsplash/event/pool/" + UUID.randomUUID(),
                            url));
                }

                if (!dynamicPool.isEmpty()) {
                    return dynamicPool;
                }
            } catch (Exception ex) {
                log.warn("Could not fetch dynamic event image pool: {}", ex.getMessage());
            }
        }

        return fallbackImagePool();
    }

    private List<ImageData> fallbackImagePool() {
        return List.of(
                new ImageData("seed-fallback", "fallback/event/1", "https://picsum.photos/seed/event-cleanup/1200/800"),
                new ImageData("seed-fallback", "fallback/event/2", "https://picsum.photos/seed/event-tree/1200/800"),
                new ImageData("seed-fallback", "fallback/event/3", "https://picsum.photos/seed/event-recycle/1200/800"),
                new ImageData("seed-fallback", "fallback/event/4", "https://picsum.photos/seed/event-education/1200/800"),
                new ImageData("seed-fallback", "fallback/event/5", "https://picsum.photos/seed/event-community/1200/800")
        );
    }

    private List<EventPlan> buildEventPlans() {
        return List.of(
                new EventPlan(
                        "Dọn rác bãi biển Vũng Tàu",
                        "Chiến dịch thu gom rác nhựa tại bãi biển với sự tham gia của cộng đồng địa phương.",
                        GreenEventType.CLEANUP,
                        "Bà Rịa - Vũng Tàu",
                        "Vũng Tàu",
                        "Phường 2",
                        "Bãi Sau, đường Thùy Vân",
                        5,
                        30L,
                        150L,
                        10.35,
                        107.08,
                        120.0),
                new EventPlan(
                        "Trồng cây phủ xanh công viên Gia Định",
                        "Hoạt động trồng cây xanh và chăm sóc mảng xanh đô thị cùng nhóm tình nguyện.",
                        GreenEventType.PLANTING,
                        "Thành phố Hồ Chí Minh",
                        "Phú Nhuận",
                        "Phường 9",
                        "Công viên Gia Định",
                        7,
                        40L,
                        200L,
                        10.811,
                        106.678,
                        140.0),
                new EventPlan(
                        "Ngày hội tái chế cộng đồng",
                        "Đổi rác lấy quà và hướng dẫn phân loại rác đúng cách tại khu dân cư.",
                        GreenEventType.RECYCLING,
                        "Hà Nội",
                        "Đống Đa",
                        "Láng Hạ",
                        "Nhà văn hóa Láng Hạ",
                        9,
                        25L,
                        120L,
                        21.023,
                        105.814,
                        90.0),
                new EventPlan(
                        "Workshop sống xanh cho học sinh",
                        "Chia sẻ các thói quen xanh dễ áp dụng và thực hành tái chế thủ công.",
                        GreenEventType.EDUCATION,
                        "Đà Nẵng",
                        "Hải Châu",
                        "Thạch Thang",
                        "Trung tâm thanh thiếu niên",
                        11,
                        20L,
                        80L,
                        16.077,
                        108.219,
                        70.0),
                new EventPlan(
                        "Chủ nhật xanh tại khu dân cư",
                        "Tổng vệ sinh, phân loại rác và cải tạo bồn cây trong khu phố.",
                        GreenEventType.OTHER,
                        "Cần Thơ",
                        "Ninh Kiều",
                        "An Bình",
                        "Nhà sinh hoạt cộng đồng",
                        13,
                        20L,
                        100L,
                        10.029,
                        105.771,
                        80.0)
        );
    }

    private UserEntity findOrganizerUser() {
        UserEntity ngoUser = findUserByUsername("ngo_tester");
        if (ngoUser != null) {
            return ngoUser;
        }

        UserEntity admin = findUserByUsername("admin");
        if (admin != null) {
            return admin;
        }

        return findUserByUsername("user1");
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .or(() -> userRepository.findByIdentifier(username + "@greenify.vn"))
                .orElse(null);
    }

    private record ImageData(String bucketName, String objectKey, String imageUrl) {
    }

    private record EventPlan(
            String title,
            String description,
            GreenEventType eventType,
            String province,
            String district,
            String ward,
            String addressDetail,
            int startInDays,
            Long minParticipants,
            Long maxParticipants,
            Double latitude,
            Double longitude,
            Double rewardPoints) {
    }
}
