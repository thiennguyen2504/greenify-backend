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

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private static final List<ImageData> IMAGE_POOL = List.of(
            new ImageData("cdn.greenify.io.vn", "images/1776610243462_96036_trongcay.jpeg", "https://cdn.greenify.io.vn/images/1776610243462_96036_trongcay.jpeg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610244816_272587_vv.jpg", "https://cdn.greenify.io.vn/images/1776610244816_272587_vv.jpg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610245634_599323.jpg", "https://cdn.greenify.io.vn/images/1776610245634_599323.jpg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610246778_environmental-volunteering_orig.jpg", "https://cdn.greenify.io.vn/images/1776610246778_environmental-volunteering_orig.jpg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610247197_GettyImages-1250317616.png", "https://cdn.greenify.io.vn/images/1776610247197_GettyImages-1250317616.png"),
            new ImageData("cdn.greenify.io.vn", "images/1776610247495_group-cleaning-workers-collecting-trash-outdoors.jpg", "https://cdn.greenify.io.vn/images/1776610247495_group-cleaning-workers-collecting-trash-outdoors.jpg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610269239_images (1).jpeg", "https://cdn.greenify.io.vn/images/1776610269239_images (1).jpeg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610269461_images (2).jpeg", "https://cdn.greenify.io.vn/images/1776610269461_images (2).jpeg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610269678_images.jpeg", "https://cdn.greenify.io.vn/images/1776610269678_images.jpeg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610269897_Leo Man planting.webp", "https://cdn.greenify.io.vn/images/1776610269897_Leo Man planting.webp"),
            new ImageData("cdn.greenify.io.vn", "images/1776610270202_recycling.jpg", "https://cdn.greenify.io.vn/images/1776610270202_recycling.jpg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610270893_srodowisko-i-radosna-koncepcja-wolontariuszy-768x512.jpg", "https://cdn.greenify.io.vn/images/1776610270893_srodowisko-i-radosna-koncepcja-wolontariuszy-768x512.jpg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610271194_tham_20231215162015.jpg", "https://cdn.greenify.io.vn/images/1776610271194_tham_20231215162015.jpg"),
            new ImageData("cdn.greenify.io.vn", "images/1776610271900_tin-forest-2.jpg", "https://cdn.greenify.io.vn/images/1776610271900_tin-forest-2.jpg")
    );

    public void seed() {
        if (eventRepository.count() > 5) {
            return;
        }

        List<UserEntity> ngos = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("NGO")))
                .toList();

        if (ngos.isEmpty()) {
            log.warn("No NGOs found to seed events.");
            return;
        }

        List<EventData> eventTemplates = List.of(
                new EventData("Dọn dẹp bãi biển Vũng Tàu", "Chung tay làm sạch bờ biển, thu gom rác thải nhựa và bảo vệ hệ sinh thái biển.", GreenEventType.CLEANUP, "Bà Rịa - Vũng Tàu", "TP. Vũng Tàu", "Phường 1", "Bãi Trước", 10.346, 107.072),
                new EventData("Trồng cây gây rừng tại Cần Giờ", "Hoạt động trồng rừng ngập mặn nhằm ứng phó với biến đổi khí hậu.", GreenEventType.PLANTING, "TP. Hồ Chí Minh", "Huyện Cần Giờ", "Xã Thạnh An", "Rừng ngập mặn Cần Giờ", 10.413, 106.953),
                new EventData("Ngày hội tái chế tại Hà Nội", "Học cách phân loại rác và đổi rác lấy quà tặng xanh.", GreenEventType.RECYCLING, "Hà Nội", "Quận Hoàn Kiếm", "Phường Hàng Đào", "Công viên Thống Nhất", 21.012, 105.847),
                new EventData("Giáo dục môi trường cho trẻ em", "Buổi nói chuyện về tầm quan trọng của việc bảo vệ thiên nhiên.", GreenEventType.EDUCATION, "Đà Nẵng", "Quận Hải Châu", "Phường Hòa Cường Bắc", "Cung thiếu nhi Đà Nẵng", 16.038, 108.222),
                new EventData("Làm sạch kênh rạch Sài Gòn", "Thu gom rác nổi trên các tuyến kênh của thành phố.", GreenEventType.CLEANUP, "TP. Hồ Chí Minh", "Quận 1", "Phường Đa Kao", "Kênh Nhiêu Lộc - Thị Nghè", 10.793, 106.697),
                new EventData("Trồng vườn rau cộng đồng", "Xây dựng vườn rau xanh cho các hộ gia đình có hoàn cảnh khó khăn.", GreenEventType.PLANTING, "Lâm Đồng", "TP. Đà Lạt", "Phường 10", "Dốc số 7", 11.946, 108.458),
                new EventData("Chiến dịch Greenify Your Home", "Hướng dẫn mọi người trang trí nhà bằng các vật liệu tái chế.", GreenEventType.RECYCLING, "Hải Phòng", "Quận Ngô Quyền", "Phường Máy Chai", "Nhà văn hóa thanh niên", 20.865, 106.683),
                new EventData("Workshop phân loại rác tại nguồn", "Kỹ năng phân loại rác hữu cơ và vô cơ.", GreenEventType.EDUCATION, "Cần Thơ", "Quận Ninh Kiều", "Phường Xuân Khánh", "Đại học Cần Thơ", 10.029, 105.768),
                new EventData("Làm sạch Thác Pongour", "Hoạt động tình nguyện dọn rác tại một trong những thác đẹp nhất Lâm Đồng.", GreenEventType.CLEANUP, "Lâm Đồng", "Huyện Đức Trọng", "Xã Tân Thành", "Thác Pongour", 11.691, 108.271),
                new EventData("Ngày hội xanh cùng sinh viên", "Chuỗi hoạt động bảo vệ môi trường dành cho giới trẻ.", GreenEventType.OTHER, "TP. Hồ Chí Minh", "TP. Thủ Đức", "Phường Linh Trung", "Làng Đại học", 10.871, 106.792)
        );

        int imgIndex = 0;
        for (int i = 0; i < eventTemplates.size(); i++) {
            EventData data = eventTemplates.get(i);
            UserEntity organizer = ngos.get(i % ngos.size());

            AddressEntity address = AddressEntity.builder()
                    .province(data.province)
                    .district(data.district)
                    .ward(data.ward)
                    .addressDetail(data.detail)
                    .latitude(BigDecimal.valueOf(data.lat))
                    .longitude(BigDecimal.valueOf(data.lng))
                    .build();

            EventEntity event = EventEntity.builder()
                    .title(data.title)
                    .description(data.desc)
                    .eventType(data.type)
                    .startTime(LocalDateTime.now().plusDays(i + 2).withHour(8).withMinute(0))
                    .endTime(LocalDateTime.now().plusDays(i + 2).withHour(17).withMinute(0))
                    .maxParticipants(50L + (i * 10))
                    .minParticipants(5L)
                    .signUpDeadlineHoursBefore(24L)
                    .cancelDeadlineHoursBefore(48L)
                    .status(GreenEventStatus.PUBLISHED)
                    .rewardPoints(100.0)
                    .organizer(organizer)
                    .address(address)
                    .build();

            // Rotate images - Thumbnail
            ImageData thumbImg = IMAGE_POOL.get(imgIndex % IMAGE_POOL.size());
            imgIndex++;

            event.getImages().add(EventImageEntity.builder()
                    .bucketName(thumbImg.bucketName)
                    .objectKey(thumbImg.objectKey)
                    .imageUrl(thumbImg.imageUrl)
                    .imageType(EventImageType.THUMBNAIL)
                    .event(event)
                    .build());

            // Rotate images - Detail
            ImageData detailImg = IMAGE_POOL.get(imgIndex % IMAGE_POOL.size());
            imgIndex++;

            event.getImages().add(EventImageEntity.builder()
                    .bucketName(detailImg.bucketName)
                    .objectKey(detailImg.objectKey)
                    .imageUrl(detailImg.imageUrl)
                    .imageType(EventImageType.DETAIL)
                    .event(event)
                    .build());

            eventRepository.save(event);
            log.info("Seeded Event: {} with Thumbnail and Detail images", data.title);
        }
    }

    private record EventData(String title, String desc, GreenEventType type, String province, String district, String ward, String detail, double lat, double lng) {}
    private record ImageData(String bucketName, String objectKey, String imageUrl) {}
}
