package com.webdev.greenify.common.util;

import com.webdev.greenify.notification.entity.NotificationEntity;
import com.webdev.greenify.notification.enumeration.NotificationType;
import com.webdev.greenify.notification.repository.NotificationRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSeed {

    private static final long SEED_THRESHOLD = 8;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void seed() {
        if (notificationRepository.count() > SEED_THRESHOLD) {
                        log.info("Bỏ qua NotificationSeed vì số lượng thông báo đã lớn hơn {}", SEED_THRESHOLD);
            return;
        }

        try {
            List<NotificationTemplate> templates = buildTemplates();
            int createdCount = 0;

            for (NotificationTemplate template : templates) {
                UserEntity user = userRepository.findByIdentifier(template.userIdentifier()).orElse(null);
                if (user == null) {
                                        log.warn("Bỏ qua seed thông báo vì không tìm thấy user {}", template.userIdentifier());
                    continue;
                }

                NotificationEntity notification = NotificationEntity.builder()
                        .user(user)
                        .title(template.title())
                        .content(template.content())
                        .type(template.type())
                        .isRead(template.isRead())
                        .targetId(template.targetId())
                        .build();

                notification.setCreatedAt(template.createdAt());
                notificationRepository.save(notification);
                createdCount++;
            }

                        log.info("NotificationSeed hoàn tất với {} thông báo", createdCount);
        } catch (Exception e) {
                        log.warn("NotificationSeed thất bại: {}", e.getMessage(), e);
        }
    }

    private List<NotificationTemplate> buildTemplates() {
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                new NotificationTemplate(
                        "user1@greenify.vn",
                        "Bài đăng đã được duyệt",
                        "Bài đăng hành động xanh của bạn đã được CTV duyệt thành công.",
                        NotificationType.EVENT_APPROVED,
                        false,
                        "post-seed-001",
                        now.minusMinutes(15)),
                new NotificationTemplate(
                        "user1@greenify.vn",
                        "Bạn nhận được điểm thưởng",
                        "Bạn vừa nhận thêm 7 điểm từ hành động xanh mới nhất.",
                        NotificationType.POINT_RECEIVED,
                        true,
                        "point-seed-001",
                        now.minusHours(2)),
                new NotificationTemplate(
                        "user2@greenify.vn",
                        "Bài đăng cần chỉnh sửa",
                        "Bài đăng của bạn bị từ chối. Vui lòng bổ sung hình ảnh rõ ràng hơn để gửi lại.",
                        NotificationType.EVENT_REJECTED,
                        false,
                        "post-seed-002",
                        now.minusHours(4)),
                new NotificationTemplate(
                        "user3@greenify.vn",
                        "Sự kiện đã tạo thành công",
                        "Sự kiện xanh của bạn đã được tạo. Hãy theo dõi đăng ký của người tham gia.",
                        NotificationType.EVENT_CREATED_SUCCESS,
                        true,
                        "event-seed-001",
                        now.minusHours(6)),
                new NotificationTemplate(
                        "user4@greenify.vn",
                        "Thông báo hệ thống",
                        "Hệ thống vừa cập nhật bộ quy tắc đánh giá bài đăng. Vui lòng xem thông tin mới.",
                        NotificationType.OTHER,
                        false,
                        "system-seed-001",
                        now.minusHours(9)),
                new NotificationTemplate(
                        "ctv1@greenify.vn",
                        "Bạn đã hoàn thành phiên duyệt",
                        "Cảm ơn bạn đã đóng góp kiểm duyệt bài đăng. Điểm cộng tác đã được ghi nhận.",
                        NotificationType.POINT_RECEIVED,
                        false,
                        "review-seed-001",
                        now.minusHours(12)),
                new NotificationTemplate(
                        "ctv2@greenify.vn",
                        "Nhiệm vụ kiểm duyệt mới",
                        "Có 3 bài đăng mới đang chờ CTV kiểm duyệt.",
                        NotificationType.OTHER,
                        true,
                        "review-seed-002",
                        now.minusDays(1).minusHours(1)),
                new NotificationTemplate(
                        "ngo@example.com",
                        "Thông báo từ hệ thống NGO",
                        "Có 1 điểm rác mới cần tổ chức của bạn tiếp nhận xử lý.",
                        NotificationType.OTHER,
                        false,
                        "trashspot-seed-001",
                        now.minusDays(1).minusHours(4)),
                new NotificationTemplate(
                        "admin@example.com",
                        "Báo cáo hệ thống hằng ngày",
                        "Báo cáo ngày: số bài đăng được duyệt và số điểm đã phân phối đã sẵn sàng.",
                        NotificationType.OTHER,
                        true,
                        "admin-seed-001",
                        now.minusDays(2)),
                new NotificationTemplate(
                        "user5@greenify.vn",
                        "Bạn nhận được điểm thưởng",
                        "Cộng đồng đã xác minh đóng góp của bạn. Điểm thưởng đã được cập nhật vào ví.",
                        NotificationType.POINT_RECEIVED,
                        false,
                        "point-seed-002",
                        now.minusDays(2).minusHours(3))
        );
    }

    private record NotificationTemplate(
            String userIdentifier,
            String title,
            String content,
            NotificationType type,
            boolean isRead,
            String targetId,
            LocalDateTime createdAt) {
    }
}