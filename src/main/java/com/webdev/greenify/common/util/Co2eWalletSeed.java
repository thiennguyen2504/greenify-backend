package com.webdev.greenify.common.util;

import com.webdev.greenify.co2e.constant.Co2eMaterialCode;
import com.webdev.greenify.co2e.entity.Co2eTransactionEntity;
import com.webdev.greenify.co2e.entity.GreenImpactWalletEntity;
import com.webdev.greenify.co2e.enumeration.Co2eTransactionStatus;
import com.webdev.greenify.co2e.enumeration.Co2eType;
import com.webdev.greenify.co2e.repository.Co2eTransactionRepository;
import com.webdev.greenify.co2e.repository.GreenImpactWalletRepository;
import com.webdev.greenify.greenaction.entity.GreenActionPostEntity;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Co2eWalletSeed {

    private static final long SEED_THRESHOLD = 8;
    private static final int MIN_PRIMARY_TRANSACTIONS = 8;
    private static final long DASHBOARD_SHOWCASE_THRESHOLD = 30;
    private static final String PRIMARY_USERNAME = "user";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final Co2eTransactionRepository co2eTransactionRepository;
    private final GreenImpactWalletRepository walletRepository;
    private final GreenActionPostRepository postRepository;

    @Transactional
    public void seed() {
        try {
            List<GreenActionPostEntity> posts = postRepository.findAll().stream().toList();
        List<Co2eMaterialCode> materialCodes = List.of(
            Co2eMaterialCode.PET_PLASTIC,
            Co2eMaterialCode.REUSABLE_BOTTLE,
            Co2eMaterialCode.CLEANUP_MIXED_WASTE,
            Co2eMaterialCode.TREE_PLANTING_VERIFIED,
            Co2eMaterialCode.DONATE_TEXTILE);

                List<GreenActionPostEntity> primaryPosts = posts.stream()
                    .filter(post -> PRIMARY_USERNAME.equalsIgnoreCase(post.getUser().getUsername()))
                    .toList();
                Map<String, GreenActionPostEntity> primaryByCaption = primaryPosts.stream()
                    .filter(post -> post.getCaption() != null)
                    .collect(java.util.stream.Collectors.toMap(
                        GreenActionPostEntity::getCaption,
                        post -> post,
                        (left, ignored) -> left));
            String primaryUserId = primaryPosts.stream()
                .map(post -> post.getUser().getId())
                .findFirst()
                .orElse(null);

            boolean needsPrimarySeed = primaryUserId != null
                && co2eTransactionRepository.countByUserId(primaryUserId) < MIN_PRIMARY_TRANSACTIONS;

            boolean needsGeneralSeed = co2eTransactionRepository.count() <= SEED_THRESHOLD;

            if (!needsPrimarySeed && !needsGeneralSeed) {
            log.info("Skip Co2eWalletSeed because sufficient data already exists");
            return;
            }

            List<Co2eMaterialCode> materialCodes1 = List.of(
                    Co2eMaterialCode.PET_PLASTIC,
                    Co2eMaterialCode.REUSABLE_BOTTLE,
                    Co2eMaterialCode.CLEANUP_MIXED_WASTE,
                    Co2eMaterialCode.TREE_PLANTING_VERIFIED,
                    Co2eMaterialCode.DONATE_TEXTILE);

                Map<String, EnumMap<Co2eType, BigDecimal>> totalsByUser = new java.util.HashMap<>();
                List<Co2eTransactionEntity> seeded = new ArrayList<>();

                int materialIndex = 0;
                int dayOffset = 0;

                    if (needsPrimarySeed && !primaryPosts.isEmpty()) {
                    int[] cursor = seedPrimaryTransactions(
                        primaryByCaption,
                        totalsByUser,
                        seeded,
                        dayOffset);
                    materialIndex = cursor[0];
                    dayOffset = cursor[1];
                }

                if (needsGeneralSeed) {
                int target = Math.max(0, (int) SEED_THRESHOLD - seeded.size());
                int[] cursor = seedTransactions(posts,
                    target,
                    materialCodes1,
                    totalsByUser,
                    seeded,
                    materialIndex,
                    dayOffset);
                materialIndex = cursor[0];
                dayOffset = cursor[1];
                }

            for (Map.Entry<String, EnumMap<Co2eType, BigDecimal>> entry : totalsByUser.entrySet()) {
                String userId = entry.getKey();
                EnumMap<Co2eType, BigDecimal> totals = entry.getValue();

                GreenImpactWalletEntity wallet = walletRepository.findByUserId(userId)
                        .orElseGet(() -> GreenImpactWalletEntity.builder().userId(userId).build());

                BigDecimal avoided = defaultZero(totals.get(Co2eType.AVOIDED));
                BigDecimal absorbed = defaultZero(totals.get(Co2eType.ABSORBED));

                wallet.setUserId(userId);
                wallet.setTotalAvoidedKg(avoided);
                wallet.setTotalAbsorbedKg(absorbed);
                walletRepository.save(wallet);
            }

            log.info("Seeded {} CO2e transactions and {} wallets", seeded.size(), totalsByUser.size());
            
            // Seed dashboard showcase data if needed
            seedDashboardShowcaseTransactions(posts);
        } catch (Exception ex) {
            log.warn("Co2eWalletSeed failed: {}", ex.getMessage(), ex);
        }
    }

    private int[] seedTransactions(
            List<GreenActionPostEntity> posts,
            int maxCount,
            List<Co2eMaterialCode> materialCodes,
            Map<String, EnumMap<Co2eType, BigDecimal>> totalsByUser,
            List<Co2eTransactionEntity> seeded,
            int materialIndex,
            int dayOffset) {

        if (maxCount <= 0) {
            return new int[]{materialIndex, dayOffset};
        }

        int postPointer = 0;
        int createdCount = 0;

        while (postPointer < posts.size() && createdCount < maxCount) {
            GreenActionPostEntity post = posts.get(postPointer++);
            String postId = post.getId();
            if (postId == null || co2eTransactionRepository.existsByPostId(postId)) {
                continue;
            }

            Co2eMaterialCode materialCode = materialCodes.get(materialIndex++ % materialCodes.size());
            BigDecimal co2eKg = BigDecimal.valueOf(materialCode.getCo2eFactorKgPerUnit())
                    .max(new BigDecimal("0.0100"));

            Co2eTransactionEntity transaction = Co2eTransactionEntity.builder()
                    .postId(postId)
                    .userId(post.getUser().getId())
                    .co2eType(materialCode.getCo2eType())
                    .co2eKg(co2eKg)
                    .status(Co2eTransactionStatus.CREDITED)
                    .materialCode(materialCode.name())
                    .materialLabel(materialCode.getLabel())
                    .confidenceScore(new BigDecimal("0.9000"))
                    .confidenceMultiplier(new BigDecimal("1.00"))
                    .estimatedWeightKg(materialCode.isWeightBased() ? new BigDecimal("0.50") : null)
                    .quantity(1)
                    .skipReason(null)
                    .build();

            transaction = co2eTransactionRepository.save(transaction);
            transaction.setCreatedAt(LocalDateTime.now().minusDays(30 - dayOffset));
            transaction = co2eTransactionRepository.save(transaction);

            seeded.add(transaction);
            totalsByUser.computeIfAbsent(transaction.getUserId(), key -> new EnumMap<>(Co2eType.class))
                    .merge(transaction.getCo2eType(), co2eKg, BigDecimal::add);

            createdCount++;
            dayOffset++;
        }

        return new int[]{materialIndex, dayOffset};
    }

        private int[] seedPrimaryTransactions(
            Map<String, GreenActionPostEntity> primaryByCaption,
            Map<String, EnumMap<Co2eType, BigDecimal>> totalsByUser,
            List<Co2eTransactionEntity> seeded,
            int dayOffset) {

        List<PrimaryCo2eSpec> specs = buildPrimaryCo2eSpecs();
        int materialIndex = 0;

        for (PrimaryCo2eSpec spec : specs) {
            GreenActionPostEntity post = primaryByCaption.get(spec.caption());
            if (post == null || post.getId() == null) {
            continue;
            }
            if (co2eTransactionRepository.existsByPostId(post.getId())) {
            continue;
            }

            Co2eMaterialCode materialCode = spec.materialCode();
            BigDecimal co2eKg = BigDecimal.valueOf(materialCode.getCo2eFactorKgPerUnit())
                .max(new BigDecimal("0.0100"));

            Co2eTransactionEntity transaction = Co2eTransactionEntity.builder()
                .postId(post.getId())
                .userId(post.getUser().getId())
                .co2eType(materialCode.getCo2eType())
                .co2eKg(co2eKg)
                .status(Co2eTransactionStatus.CREDITED)
                .materialCode(materialCode.name())
                .materialLabel(materialCode.getLabel())
                .confidenceScore(new BigDecimal("0.9000"))
                .confidenceMultiplier(new BigDecimal("1.00"))
                .estimatedWeightKg(materialCode.isWeightBased() ? new BigDecimal("0.50") : null)
                .quantity(1)
                .skipReason(null)
                .build();

            transaction = co2eTransactionRepository.save(transaction);
            transaction.setCreatedAt(LocalDateTime.now().minusDays(7 - dayOffset));
            transaction = co2eTransactionRepository.save(transaction);

            seeded.add(transaction);
            totalsByUser.computeIfAbsent(transaction.getUserId(), key -> new EnumMap<>(Co2eType.class))
                .merge(transaction.getCo2eType(), co2eKg, BigDecimal::add);

            dayOffset++;
            materialIndex++;
        }

        return new int[]{materialIndex, dayOffset};
        }

        private List<PrimaryCo2eSpec> buildPrimaryCo2eSpecs() {
        return List.of(
            new PrimaryCo2eSpec(
                "Quyên góp 2 thùng quần áo cũ cho trung tâm tiếp nhận đồ tái sử dụng.",
                Co2eMaterialCode.DONATE_TEXTILE),
            new PrimaryCo2eSpec(
                "Trồng thêm 3 cây xanh tại sân chung cư và gắn biển chăm sóc.",
                Co2eMaterialCode.TREE_PLANTING_VERIFIED),
            new PrimaryCo2eSpec(
                "Tham gia nhặt rác cuối tuần, thu gom 5 túi rác hỗn hợp tại công viên.",
                Co2eMaterialCode.CLEANUP_MIXED_WASTE),
            new PrimaryCo2eSpec(
                "Mang bình nước cá nhân khi đi làm để giảm chai nhựa dùng một lần.",
                Co2eMaterialCode.REUSABLE_BOTTLE),
            new PrimaryCo2eSpec(
                "Thu gom chai nhựa PET từ văn phòng và đem đến điểm tái chế.",
                Co2eMaterialCode.PET_PLASTIC)
        );
        }

        private record PrimaryCo2eSpec(String caption, Co2eMaterialCode materialCode) {
        }

    private void seedDashboardShowcaseTransactions(List<GreenActionPostEntity> posts) {
        try {
            long totalTransactions = co2eTransactionRepository.count();
            if (totalTransactions >= DASHBOARD_SHOWCASE_THRESHOLD) {
                log.info("Skip dashboard showcase seed because sufficient data already exists");
                return;
            }

            List<Co2eMaterialCode> materialCodes = List.of(
                    Co2eMaterialCode.PET_PLASTIC,
                    Co2eMaterialCode.REUSABLE_BOTTLE,
                    Co2eMaterialCode.CLEANUP_MIXED_WASTE,
                    Co2eMaterialCode.TREE_PLANTING_VERIFIED,
                    Co2eMaterialCode.DONATE_TEXTILE);

            Map<String, EnumMap<Co2eType, BigDecimal>> totalsByUser = new java.util.HashMap<>();
            List<Co2eTransactionEntity> seeded = new ArrayList<>();
            int materialIndex = 0;
            int targetCount = (int) (DASHBOARD_SHOWCASE_THRESHOLD - totalTransactions);

            int postPointer = 0;
            int createdCount = 0;
            
            // Create recent transactions with timestamps spread across recent days
            while (postPointer < posts.size() && createdCount < targetCount) {
                GreenActionPostEntity post = posts.get(postPointer++);
                String postId = post.getId();
                if (postId == null || co2eTransactionRepository.existsByPostId(postId)) {
                    continue;
                }

                Co2eMaterialCode materialCode = materialCodes.get(materialIndex++ % materialCodes.size());
                BigDecimal co2eKg = BigDecimal.valueOf(materialCode.getCo2eFactorKgPerUnit())
                        .max(new BigDecimal("0.0100"));

                Co2eTransactionEntity transaction = Co2eTransactionEntity.builder()
                        .postId(postId)
                        .userId(post.getUser().getId())
                        .co2eType(materialCode.getCo2eType())
                        .co2eKg(co2eKg)
                        .status(Co2eTransactionStatus.CREDITED)
                        .materialCode(materialCode.name())
                        .materialLabel(materialCode.getLabel())
                        .confidenceScore(new BigDecimal("0.9000"))
                        .confidenceMultiplier(new BigDecimal("1.00"))
                        .estimatedWeightKg(materialCode.isWeightBased() ? new BigDecimal("0.50") : null)
                        .quantity(1)
                        .skipReason(null)
                        .build();

                transaction = co2eTransactionRepository.save(transaction);
                // Spread recent transactions across last 7 days for nice dashboard display
                int daysAgo = (createdCount % 7) + 1;
                transaction.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
                transaction = co2eTransactionRepository.save(transaction);

                seeded.add(transaction);
                totalsByUser.computeIfAbsent(transaction.getUserId(), key -> new EnumMap<>(Co2eType.class))
                        .merge(transaction.getCo2eType(), co2eKg, BigDecimal::add);

                createdCount++;
            }

            // Update wallets with new transactions
            for (Map.Entry<String, EnumMap<Co2eType, BigDecimal>> entry : totalsByUser.entrySet()) {
                String userId = entry.getKey();
                EnumMap<Co2eType, BigDecimal> totals = entry.getValue();

                GreenImpactWalletEntity wallet = walletRepository.findByUserId(userId)
                        .orElseGet(() -> GreenImpactWalletEntity.builder().userId(userId).build());

                BigDecimal currentAvoided = wallet.getTotalAvoidedKg() != null ? wallet.getTotalAvoidedKg() : ZERO;
                BigDecimal currentAbsorbed = wallet.getTotalAbsorbedKg() != null ? wallet.getTotalAbsorbedKg() : ZERO;

                BigDecimal newAvoided = currentAvoided.add(defaultZero(totals.get(Co2eType.AVOIDED)));
                BigDecimal newAbsorbed = currentAbsorbed.add(defaultZero(totals.get(Co2eType.ABSORBED)));

                wallet.setTotalAvoidedKg(newAvoided);
                wallet.setTotalAbsorbedKg(newAbsorbed);
                walletRepository.save(wallet);
            }

            log.info("Seeded {} dashboard showcase CO2e transactions", seeded.size());
        } catch (Exception ex) {
            log.warn("Dashboard showcase seed failed: {}", ex.getMessage(), ex);
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
