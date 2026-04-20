package com.webdev.greenify.common.util;

import com.webdev.greenify.station.entity.AddressEntity;
import com.webdev.greenify.user.entity.NGOProfileEntity;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.enumeration.NGOProfileStatus;
import com.webdev.greenify.user.repository.NGOProfileRepository;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NGOSeed {

    private final UserRepository userRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public void seed() {
        RoleEntity ngoRole = roleRepository.findByName("NGO").orElseThrow();
        RoleEntity userRole = roleRepository.findByName("USER").orElseThrow();

        List<NGOData> ngos = List.of(
                new NGOData("Hành Tinh Xanh Foundation", "ngo@example.com", "Nguyễn Văn A", "0912345678", "contact@hanhtinhxanh.org", "Tổ chức phi lợi nhuận vì môi trường xanh.", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Lợi"),
                new NGOData("Vietnam Clean & Green", "ngob@example.com", "Trần Thị B", "0987654321", "info@cleanandgreen.vn", "Chung tay bảo vệ môi trường Việt Nam.", "Hà Nội", "Quận Đống Đa", "Phường Láng Hạ", "456 Đường Láng"),
                new NGOData("Vì Một Việt Nam Xanh", "ngoc@example.com", "Lê Văn C", "0909090909", "vimoivietnamxanh@charity.vn", "Phủ xanh đồi trọc và làm sạch biển.", "Đà Nẵng", "Quận Liên Chiểu", "Phường Hòa Khánh Bắc", "789 Nguyễn Lương Bằng"),
                new NGOData("Green Life Organization", "ngod@example.com", "Phạm Văn D", "0888888888", "greenlife@ngo.org", "Khuyến khích lối sống xanh bền vững.", "Cần Thơ", "Quận Ninh Kiều", "Phường An Khánh", "101 Nguyễn Văn Cừ"),
                new NGOData("Eco Warriors", "ngoe@example.com", "Hoàng Thị E", "0777777777", "ecowarriors@volunteers.vn", "Đội ngũ chiến binh bảo vệ hệ sinh thái.", "Lâm Đồng", "TP. Đà Lạt", "Phường 1", "202 Phan Đình Phùng")
        );

        for (NGOData data : ngos) {
            String email = data.email;
            if (userRepository.findByIdentifier(email).isEmpty()) {
                Set<RoleEntity> roles = new HashSet<>();
                roles.add(ngoRole);
                roles.add(userRole);

                UserEntity user = UserEntity.builder()
                        .email(email)
                        .username(email.split("@")[0])
                        .password(passwordEncoder.encode("password123"))
                        .roles(roles)
                        .status(AccountStatus.ACTIVE)
                        .build();
                user = userRepository.save(user);

                AddressEntity address = AddressEntity.builder()
                        .province(data.province)
                        .district(data.district)
                        .ward(data.ward)
                        .addressDetail(data.addressDetail)
                        .build();

                NGOProfileEntity profile = NGOProfileEntity.builder()
                        .orgName(data.name)
                        .representativeName(data.rep)
                        .hotline(data.hotline)
                        .contactEmail(data.contactEmail)
                        .description(data.desc)
                        .status(NGOProfileStatus.VERIFIED)
                        .user(user)
                        .address(address)
                        .rejectedCount(0)
                        .build();
                ngoProfileRepository.save(profile);
                log.info("Seeded NGO: {}", data.name);
            }
        }
    }

    private record NGOData(String name, String email, String rep, String hotline, String contactEmail, String desc, String province, String district, String ward, String addressDetail) {}
}
