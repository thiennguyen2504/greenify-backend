package com.webdev.greenify.common.util;

import com.webdev.greenify.file.entity.PostImageEntity;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.entity.PointTransactionEntity;
import com.webdev.greenify.greenaction.entity.PostAppealEntity;
import com.webdev.greenify.greenaction.entity.PostReviewEntity;
import com.webdev.greenify.greenaction.enumeration.AppealStatus;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.enumeration.ReviewDecision;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.greenaction.repository.PostAppealRepository;
import com.webdev.greenify.greenaction.repository.PostReviewRepository;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.UserProfileEntity;
import com.webdev.greenify.user.enumeration.ImageStatus;
import com.webdev.greenify.user.repository.UserProfileRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostSeed {

    private static final long SEED_THRESHOLD = 10;
    private static final int TOTAL_VERIFIED = 10;
    private static final int TOTAL_PENDING = 8;
    private static final int TOTAL_REJECTED = 5;
    private static final int TOTAL_REJECTED_WITH_APPEAL = 5;
    private static final long LEGACY_APPEAL_COUNT = 2;
    private static final BigDecimal REVIEWER_BONUS_POINTS = new BigDecimal("1.00");

    private final GreenActionPostRepository postRepository;
    private final PostReviewRepository reviewRepository;
    private final GreenActionTypeRepository actionTypeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PostAppealRepository postAppealRepository;
    private final UnsplashImageService unsplashImageService;

    private final Map<String, List<String>> postImageCache = new HashMap<>();
    private final Map<String, AtomicInteger> postImageCursor = new HashMap<>();

    @Transactional
    public void seed() {
        topUpLegacyAppealsIfNeeded();

        if (postRepository.count() > SEED_THRESHOLD) {
            log.info("Skip PostSeed because post count is already greater than {}", SEED_THRESHOLD);
            return;
        }

        try {
            List<GreenActionTypeEntity> actionTypes = actionTypeRepository.findAllByOrderByGroupNameAscActionNameAsc();
            if (actionTypes.isEmpty()) {
                log.warn("Skip PostSeed because no green action types found");
                return;
            }

            prefetchPostImages(actionTypes);

            List<UserEntity> reviewers = loadUsersByUsernames(List.of("ctv1", "ctv2", "ctv3"));
            if (reviewers.size() < 3) {
                log.warn("Skip PostSeed because not enough CTV reviewers (need 3, found {})", reviewers.size());
                return;
            }

            List<UserEntity> authors = loadUsersByUsernames(List.of(
                    "user1", "user2", "user3", "user4", "user5", "user6", "user7", "ctv1", "ctv2", "ctv3"
            ));
            if (authors.isEmpty()) {
                log.warn("Skip PostSeed because no authors found");
                return;
            }

            int imageIndex = 1;
            int seededCount = 0;

            for (int i = 0; i < TOTAL_VERIFIED; i++) {
                try {
                    seededCount++;
                    imageIndex = seedVerifiedPost(i, imageIndex, authors, reviewers, actionTypes);
                } catch (Exception ex) {
                    log.warn("Skip verified post index {} due to error: {}", i, ex.getMessage());
                }
            }

            for (int i = 0; i < TOTAL_PENDING; i++) {
                try {
                    seededCount++;
                    imageIndex = seedPendingPost(i, imageIndex, authors, reviewers, actionTypes);
                } catch (Exception ex) {
                    log.warn("Skip pending post index {} due to error: {}", i, ex.getMessage());
                }
            }

            for (int i = 0; i < TOTAL_REJECTED; i++) {
                try {
                    seededCount++;
                    imageIndex = seedRejectedPost(i, imageIndex, authors, reviewers, actionTypes, false);
                } catch (Exception ex) {
                    log.warn("Skip rejected post index {} due to error: {}", i, ex.getMessage());
                }
            }

            for (int i = 0; i < TOTAL_REJECTED_WITH_APPEAL; i++) {
                try {
                    seededCount++;
                    imageIndex = seedRejectedPost(i, imageIndex, authors, reviewers, actionTypes, true);
                } catch (Exception ex) {
                    log.warn("Skip rejected-with-appeal post index {} due to error: {}", i, ex.getMessage());
                }
            }

            log.info("PostSeed completed with {} intended records", seededCount);
        } catch (Exception e) {
            log.warn("PostSeed failed: {}", e.getMessage(), e);
        }
    }

    private void topUpLegacyAppealsIfNeeded() {
        long currentAppealCount = postAppealRepository.count();
        if (currentAppealCount != LEGACY_APPEAL_COUNT) {
            return;
        }

        int additionalAppealsNeeded = TOTAL_REJECTED_WITH_APPEAL - (int) currentAppealCount;
        if (additionalAppealsNeeded <= 0) {
            return;
        }

        List<GreenActionTypeEntity> actionTypes = actionTypeRepository.findAllByOrderByGroupNameAscActionNameAsc();
        if (actionTypes.isEmpty()) {
            log.warn("Skip legacy appeal top-up because no green action types found");
            return;
        }

        prefetchPostImages(actionTypes);

        List<UserEntity> reviewers = loadUsersByUsernames(List.of("ctv1", "ctv2", "ctv3"));
        if (reviewers.size() < 3) {
            log.warn("Skip legacy appeal top-up because not enough CTV reviewers (need 3, found {})", reviewers.size());
            return;
        }

        List<UserEntity> authors = loadUsersByUsernames(List.of(
                "user1", "user2", "user3", "user4", "user5", "user6", "user7", "ctv1", "ctv2", "ctv3"
        ));
        if (authors.isEmpty()) {
            log.warn("Skip legacy appeal top-up because no authors found");
            return;
        }

        int seeded = 0;
        int imageIndex = 1;
        int baseIndex = (int) currentAppealCount;

        for (int i = 0; i < additionalAppealsNeeded; i++) {
            try {
                imageIndex = seedRejectedPost(baseIndex + i, imageIndex, authors, reviewers, actionTypes, true);
                seeded++;
            } catch (Exception ex) {
                log.warn("Skip legacy appeal top-up index {} due to error: {}", i, ex.getMessage());
            }
        }

        log.info("Legacy appeal top-up completed: added {} pending appeals (from {} to target {})",
                seeded, currentAppealCount, TOTAL_REJECTED_WITH_APPEAL);
    }

    private int seedVerifiedPost(
            int index,
            int imageIndex,
            List<UserEntity> authors,
            List<UserEntity> reviewers,
            List<GreenActionTypeEntity> actionTypes) {

        UserEntity author = authors.get(index % authors.size());
        GreenActionTypeEntity actionType = actionTypes.get(index % actionTypes.size());
        LocalDateTime actionAt = randomDateWithin(30);

        GreenActionPostEntity post = buildPost(author, actionType, captionAt(index), PostStatus.VERIFIED, 3, 0, actionAt);
        post = postRepository.save(post);

        List<PostReviewEntity> savedReviews = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            savedReviews.add(saveReview(post, reviewers.get(i), ReviewDecision.APPROVE, null));
        }

        createAuthorPointTransaction(post, author, actionAt.plusHours(3));
        for (PostReviewEntity review : savedReviews) {
            createReviewerPointTransaction(post, review, actionAt.plusHours(4));
        }

        log.info("Seeded VERIFIED post: {}", post.getId());
        return imageIndex + 1;
    }

    private int seedPendingPost(
            int index,
            int imageIndex,
            List<UserEntity> authors,
            List<UserEntity> reviewers,
            List<GreenActionTypeEntity> actionTypes) {

        UserEntity author = authors.get((index + 3) % authors.size());
        GreenActionTypeEntity actionType = actionTypes.get((index + 5) % actionTypes.size());
        LocalDateTime actionAt = randomDateWithin(20);

        int approveCount = ThreadLocalRandom.current().nextBoolean() ? 1 : 2;
        int rejectCount = ThreadLocalRandom.current().nextBoolean() ? 1 : 0;

        GreenActionPostEntity post = buildPost(
                author,
                actionType,
                captionAt(index + 10),
                PostStatus.PENDING_REVIEW,
                approveCount,
                rejectCount,
            actionAt);
        post = postRepository.save(post);

        List<UserEntity> shuffledReviewers = new ArrayList<>(reviewers);
        Collections.shuffle(shuffledReviewers);

        for (int i = 0; i < approveCount; i++) {
            saveReview(post, shuffledReviewers.get(i), ReviewDecision.APPROVE, null);
        }

        if (rejectCount > 0) {
            saveReview(post, shuffledReviewers.get(approveCount), ReviewDecision.REJECT, rejectReasonAt(index));
        }

        log.info("Seeded PENDING_REVIEW post: {}", post.getId());
        return imageIndex + 1;
    }

    private int seedRejectedPost(
            int index,
            int imageIndex,
            List<UserEntity> authors,
            List<UserEntity> reviewers,
            List<GreenActionTypeEntity> actionTypes,
            boolean withAppeal) {

        UserEntity author = authors.get((index + 6) % authors.size());
        GreenActionTypeEntity actionType = actionTypes.get((index + 2) % actionTypes.size());
        LocalDateTime actionAt = randomDateWithin(25);

        GreenActionPostEntity post = buildPost(
                author,
                actionType,
                captionAt(index + 20),
                PostStatus.REJECTED,
                0,
                3,
            actionAt);
        post = postRepository.save(post);

        if (index % 2 == 0) {
            saveReview(post, reviewers.get(0), ReviewDecision.REPORT_SUSPICIOUS,
                    "Nghi ngờ ảnh không đúng ngữ cảnh hoạt động xanh.");
            saveReview(post, reviewers.get(1), ReviewDecision.REJECT, rejectReasonAt(index));
            saveReview(post, reviewers.get(2), ReviewDecision.REJECT, rejectReasonAt(index + 1));
        } else {
            for (int i = 0; i < 3; i++) {
                saveReview(post, reviewers.get(i), ReviewDecision.REJECT, rejectReasonAt(index + i));
            }
        }

        if (withAppeal) {
            PostAppealEntity appeal = PostAppealEntity.builder()
                    .post(post)
                    .user(author)
                    .appealReason(appealReasonAt(index))
                    .evidenceUrls(buildAppealEvidenceUrls(index))
                    .attemptNumber(1)
                    .status(AppealStatus.APPEAL_SUBMITTED)
                    .build();
            postAppealRepository.save(appeal);
        }

        log.info("Seeded REJECTED post: {}", post.getId());
        return imageIndex + 1;
    }

    private GreenActionPostEntity buildPost(
            UserEntity author,
            GreenActionTypeEntity actionType,
            String caption,
            PostStatus status,
            int approveCount,
            int rejectCount,
            LocalDateTime actionAt) {

        String province = userProfileRepository.findByUserId(author.getId())
                .map(UserProfileEntity::getProvince)
                .orElse("Thành phố Hồ Chí Minh");

        GreenActionPostEntity post = GreenActionPostEntity.builder()
                .user(author)
                .actionType(actionType)
                .caption(caption)
                .actionDate(actionAt.toLocalDate())
                .status(status)
                .approveCount(approveCount)
                .rejectCount(rejectCount)
                .location(province + ", Việt Nam")
                .latitude(BigDecimal.valueOf(10.75 + ThreadLocalRandom.current().nextDouble(0.35)))
                .longitude(BigDecimal.valueOf(106.60 + ThreadLocalRandom.current().nextDouble(0.35)))
                .build();

        PostImageEntity image = buildMockImage(post, actionType);
        post.setPostImage(image);
        image.setPost(post);

        return post;
    }

    private PostReviewEntity saveReview(
            GreenActionPostEntity post,
            UserEntity reviewer,
            ReviewDecision decision,
            String rejectReason) {

        PostReviewEntity review = PostReviewEntity.builder()
                .post(post)
                .reviewer(reviewer)
                .decision(decision)
                .rejectReason(rejectReason)
                .isValid(true)
                .build();

        return reviewRepository.save(review);
    }

    private void createAuthorPointTransaction(GreenActionPostEntity post, UserEntity author, LocalDateTime earnedAt) {
        BigDecimal points = post.getActionType() != null && post.getActionType().getSuggestedPoints() != null
                ? post.getActionType().getSuggestedPoints()
                : BigDecimal.ONE;

        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .user(author)
                .points(points)
                .actionDescription("Hoàn thành hành động xanh: " + post.getActionType().getActionName())
                .sourcePostId(post.getId())
                .expiresAt(earnedAt.plusMonths(2))
                .build();
        transaction.setCreatedAt(earnedAt);

        pointTransactionRepository.save(transaction);
    }

    private void createReviewerPointTransaction(GreenActionPostEntity post, PostReviewEntity review, LocalDateTime earnedAt) {
        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .user(review.getReviewer())
                .points(REVIEWER_BONUS_POINTS)
                .actionDescription("Duyệt bài hợp lệ với tư cách CTV")
                .sourcePostId(post.getId())
                .sourceReviewId(review.getId())
                .expiresAt(earnedAt.plusMonths(2))
                .build();
        transaction.setCreatedAt(earnedAt);

        pointTransactionRepository.save(transaction);
    }

    private LocalDateTime randomDateWithin(int maxDaysAgo) {
        return LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(3, maxDaysAgo + 1));
    }

    private PostImageEntity buildMockImage(GreenActionPostEntity post, GreenActionTypeEntity actionType) {
        String keyword = resolveActionKeyword(actionType);
        String imageUrl = nextPostImageUrl(keyword);
        String normalizedKeyword = normalizeKeywordForKey(keyword);

        return PostImageEntity.builder()
                .post(post)
                .bucketName("unsplash-cdn")
                .objectKey("unsplash/" + normalizedKeyword + "/" + UUID.randomUUID())
                .imageUrl(imageUrl)
                .status(ImageStatus.ACTIVE)
                .build();
    }

    private void prefetchPostImages(List<GreenActionTypeEntity> actionTypes) {
        postImageCache.clear();
        postImageCursor.clear();

        Set<String> keywords = new LinkedHashSet<>();
        for (GreenActionTypeEntity actionType : actionTypes) {
            keywords.add(resolveActionKeyword(actionType));
        }

        int countPerKeyword = Math.max(1, (int) Math.ceil(25.0 / Math.max(1, keywords.size())));
        int prefetched = 0;

        for (String keyword : keywords) {
            List<String> urls = unsplashImageService.getMultipleImageUrls(keyword, countPerKeyword);
            postImageCache.put(keyword, urls);
            postImageCursor.put(keyword, new AtomicInteger(0));
            prefetched += urls.size();
        }

        log.info("PostSeed prefetched {} image URLs for {} keywords", prefetched, keywords.size());
    }

    private String resolveActionKeyword(GreenActionTypeEntity actionType) {
        if (actionType == null) {
            return "environment,green";
        }
        return UnsplashKeywordMapper.getActionKeyword(actionType.getGroupName());
    }

    private String nextPostImageUrl(String keyword) {
        List<String> urls = postImageCache.get(keyword);
        if (urls == null || urls.isEmpty()) {
            return unsplashImageService.getImageUrl(keyword);
        }

        AtomicInteger cursor = postImageCursor.computeIfAbsent(keyword, k -> new AtomicInteger(0));
        int nextIndex = Math.floorMod(cursor.getAndIncrement(), urls.size());
        return urls.get(nextIndex);
    }

    private String normalizeKeywordForKey(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "environment-green";
        }

        return keyword.toLowerCase()
                .replace(',', '-')
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "");
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .or(() -> userRepository.findByIdentifier(username + "@greenify.vn"))
                .orElse(null);
    }

    private List<UserEntity> loadUsersByUsernames(List<String> usernames) {
        List<UserEntity> users = new ArrayList<>();
        for (String username : usernames) {
            UserEntity user = findUserByUsername(username);
            if (user == null) {
                log.warn("Missing user for PostSeed: {}", username);
                continue;
            }
            users.add(user);
        }
        return users;
    }

    private String captionAt(int index) {
        List<String> captions = List.of(
                "Hôm nay mình đã phân loại rác thành 3 nhóm trước khi bỏ vào thùng 🌱",
                "Đi làm bằng xe đạp tiết kiệm xăng và bảo vệ môi trường!",
                "Dùng túi vải thay túi ni-lông khi đi chợ buổi sáng",
                "Tắt điện và thiết bị điện khi rời khỏi phòng",
                "Thu gom 2kg giấy báo cũ đem đến điểm tái chế",
                "Trồng thêm một chậu cây xanh trên ban công",
                "Từ chối ống hút nhựa, dùng ống hút inox thay thế",
                "Ủ phân compost từ vỏ rau củ trong bếp",
                "Dùng hộp cơm cá nhân thay hộp xốp khi mua cơm",
                "Tham gia dọn rác bãi biển cùng 30 tình nguyện viên"
        );
        return captions.get(index % captions.size());
    }

    private String rejectReasonAt(int index) {
        List<String> reasons = List.of(
            "Ảnh chụp thùng rác tái chế bị ngược sáng và mất nét; không nhìn rõ nhãn phân loại hữu cơ, tái chế, còn lại như mô tả.",
            "Bài đăng ghi đã đi xe đạp đến công ty lúc 7:30, nhưng ảnh đính kèm là ảnh bãi giữ xe máy trong hầm, không có xe đạp của người đăng.",
            "Mô tả nói đã thu gom 2kg pin cũ tại điểm tiếp nhận quận 3, tuy nhiên ảnh chỉ có 3 viên pin rời và không thấy cân/biên nhận bàn giao.",
            "Ảnh check-in dọn rác bờ kênh không có dấu hiệu thời gian hiện tại; metadata ảnh cho thấy tệp được tạo từ tháng trước.",
            "Nội dung nêu thay túi ni-lon bằng túi vải khi đi chợ, nhưng ảnh là quầy thanh toán với nhiều túi nhựa dùng một lần còn mới.",
            "Bài đăng mô tả trồng 15 cây con tại sân trường, nhưng ảnh chỉ thể hiện khu vực đất trống chưa có cây hoặc dụng cụ làm vườn."
        );
        return reasons.get(index % reasons.size());
    }

    private String appealReasonAt(int index) {
        List<String> reasons = List.of(
            "Mình đã bổ sung 2 ảnh chụp liên tiếp tại điểm thu gom pin ở 152 Nguyen Dinh Chieu, có ảnh cân hiển thị 2.1kg và phiếu xác nhận của nhân viên trực quầy.",
            "Ảnh trước bị thiếu góc chụp nên gây hiểu nhầm. Mình đã cập nhật ảnh toàn cảnh khu vực dọn rác bờ kênh Tau Hu lúc 06:15, kèm ảnh bao rác sau khi thu gom.",
            "Bài đi xe đạp có dữ liệu từ app Strava và ảnh chụp xe trước cổng tòa nhà Etown lúc 07:28. Mình gửi lại ảnh rõ biển số xe đạp và vị trí gửi xe.",
            "Mình đã bổ sung video ngắn cắt khung thành ảnh chứng minh quá trình phân loại rác tại nhà, thể hiện đầy đủ 3 thùng và lượng rác từng loại sau khi phân loại.",
            "Trong ảnh cũ chưa thấy túi vải do mình để dưới giỏ xe. Mình đã cập nhật thêm ảnh hóa đơn mua hàng và ảnh túi vải chứa thực phẩm sau khi thanh toán.",
            "Mình có đính kèm ảnh chụp theo mốc thời gian từ 08:00 đến 09:30 khi trồng cây tại sân trường, có giáo viên phụ trách xác nhận hoạt động diễn ra trong ngày."
        );
        return reasons.get(index % reasons.size());
    }

        private List<String> buildAppealEvidenceUrls(int index) {
        List<String> evidenceKeywords = List.of(
            "recycling center,receipt,battery collection",
            "canal cleanup,trash bags,community volunteer",
            "bicycle commute,office entrance,morning",
            "waste sorting,kitchen bins,recycling",
            "market reusable bag,grocery receipt,checkout",
            "school tree planting,students,community"
        );

        String primaryKeyword = evidenceKeywords.get(index % evidenceKeywords.size());
        String secondaryKeyword = evidenceKeywords.get((index + 2) % evidenceKeywords.size());

        return List.of(
            unsplashImageService.getImageUrl(primaryKeyword),
            unsplashImageService.getImageUrl(secondaryKeyword));
        }
}
