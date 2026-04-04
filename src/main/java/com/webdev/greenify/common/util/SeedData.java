package com.webdev.greenify.common.util;

import com.webdev.greenify.greenaction.entity.GreenActionTypeEntity;
import com.webdev.greenify.greenaction.repository.GreenActionTypeRepository;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeedData implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GreenActionTypeRepository greenActionTypeRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // Ensure roleEntities exist
        RoleEntity adminRoleEntity = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ADMIN").build()));

        RoleEntity userRoleEntity = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("USER").build()));

        RoleEntity ctvRoleEntity = roleRepository.findByName("CTV")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("CTV").build()));

        // Create Admin UserEntity
        if (userRepository.findByIdentifier("admin@example.com").isEmpty()) {
            Set<RoleEntity> adminRoleEntities = new HashSet<>();
            adminRoleEntities.add(adminRoleEntity);
            adminRoleEntities.add(userRoleEntity);

            userRepository.save(UserEntity.builder()
                    .email("admin@example.com")
                    .username("admin")
                    .password(passwordEncoder.encode("password123"))
                    .roles(adminRoleEntities)
                    .status(AccountStatus.ACTIVE)
                    .build());
        }

        // Create Normal UserEntity
        if (userRepository.findByIdentifier("user@example.com").isEmpty()) {
            Set<RoleEntity> userRoleEntities = new HashSet<>();
            userRoleEntities.add(userRoleEntity);

            userRepository.save(UserEntity.builder()
                    .email("user@example.com")
                    .username("user")
                    .password(passwordEncoder.encode("password123"))
                    .roles(userRoleEntities)
                    .status(AccountStatus.ACTIVE)
                    .build());
        }

        // Seed Green Action Types
        seedGreenActionTypes();
    }

    private void seedGreenActionTypes() {
        if (greenActionTypeRepository.count() == 0) {
            List<GreenActionTypeEntity> actionTypes = new ArrayList<>();

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Phân loại rác")
                    .actionName("Phân loại rác tại nhà")
                    .suggestedPoints(new BigDecimal("5"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Phân loại rác")
                    .actionName("Phân loại rác tại nơi làm việc / trường học")
                    .suggestedPoints(new BigDecimal("6"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tái chế")
                    .actionName("Gom giấy/plastic/lon để tái chế")
                    .suggestedPoints(new BigDecimal("5"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tái chế")
                    .actionName("Mang rác tái chế đến điểm thu gom")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Giảm nhựa")
                    .actionName("Sử dụng bình nước cá nhân")
                    .suggestedPoints(new BigDecimal("2"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Giảm nhựa")
                    .actionName("Dùng hộp đựng / ly cá nhân khi mua đồ ăn, nước uống")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Giảm nhựa")
                    .actionName("Từ chối túi nilon / ống hút nhựa")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tiết kiệm tài nguyên")
                    .actionName("Tắt điện khi không sử dụng")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tiết kiệm tài nguyên")
                    .actionName("Tiết kiệm nước trong sinh hoạt")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Di chuyển xanh")
                    .actionName("Đi bộ / xe đạp cho quãng đường ngắn")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Di chuyển xanh")
                    .actionName("Sử dụng phương tiện công cộng")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Làm sạch môi trường")
                    .actionName("Nhặt rác tại khu vực công cộng")
                    .suggestedPoints(new BigDecimal("8"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Làm sạch môi trường")
                    .actionName("Dọn vệ sinh khu vực sống")
                    .suggestedPoints(new BigDecimal("6"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Trồng xanh")
                    .actionName("Trồng cây / trồng hoa")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Trồng xanh")
                    .actionName("Chăm sóc cây xanh định kỳ")
                    .suggestedPoints(new BigDecimal("2"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tái sử dụng")
                    .actionName("Tái sử dụng đồ vật thay vì bỏ đi")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tiêu dùng xanh")
                    .actionName("Mua sản phẩm thân thiện môi trường")
                    .suggestedPoints(new BigDecimal("3"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tham gia cộng đồng")
                    .actionName("Tham gia sự kiện môi trường do app/NGO tổ chức")
                    .suggestedPoints(new BigDecimal("10"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Tuyên truyền xanh")
                    .actionName("Chia sẻ/tuyên truyền thông điệp môi trường có hành động thực tế đi kèm")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Sáng kiến xanh")
                    .actionName("Tổ chức hoặc khởi xướng hoạt động xanh quy mô nhỏ")
                    .suggestedPoints(new BigDecimal("9"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Báo cáo môi trường")
                    .actionName("Raise bãi rác / điểm ô nhiễm cần xử lý")
                    .suggestedPoints(new BigDecimal("4"))
                    .locationRequired(true)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Sáng tạo tái chế")
                    .actionName("Tự làm sản phẩm từ vật liệu tái chế")
                    .suggestedPoints(new BigDecimal("7"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Hữu cơ tại nhà")
                    .actionName("Ủ rác hữu cơ / làm compost đơn giản")
                    .suggestedPoints(new BigDecimal("8"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            actionTypes.add(GreenActionTypeEntity.builder()
                    .groupName("Đóng góp cộng đồng")
                    .actionName("Duyệt bài hợp lệ với tư cách CTV")
                    .suggestedPoints(new BigDecimal("1"))
                    .locationRequired(false)
                    .isActive(true)
                    .build());

            greenActionTypeRepository.saveAll(actionTypes);
            log.info("Seeded {} green action types", actionTypes.size());
        }
    }
}
