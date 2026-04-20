package com.webdev.greenify.user.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.point.entity.PointWalletEntity;
import com.webdev.greenify.point.repository.PointWalletRepository;
import com.webdev.greenify.user.dto.ChangeUserRoleRequestDTO;
import com.webdev.greenify.user.dto.CtvEligibilityResponseDTO;
import com.webdev.greenify.user.dto.DemoteCtvRequestDTO;
import com.webdev.greenify.user.dto.PagedResponse;
import com.webdev.greenify.user.dto.SuspendUserRequestDTO;
import com.webdev.greenify.user.dto.UserAdminSummaryResponseDTO;
import com.webdev.greenify.user.dto.UserDetailResponseDTO;
import com.webdev.greenify.user.dto.UserProfileResponseDTO;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.UserManagementActionEntity;
import com.webdev.greenify.user.enumeration.AccountStatus;
import com.webdev.greenify.user.enumeration.RoleName;
import com.webdev.greenify.user.enumeration.UserManagementActionType;
import com.webdev.greenify.user.mapper.NGOProfileMapper;
import com.webdev.greenify.user.mapper.UserMapper;
import com.webdev.greenify.user.mapper.UserProfileMapper;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserManagementActionRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.user.service.UserService;
import com.webdev.greenify.user.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String ROLE_USER = "USER";
    private static final String ROLE_CTV = "CTV";
    private static final String ROLE_NGO = "NGO";
    private static final BigDecimal MIN_CTV_ACCUMULATED_POINTS = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String DEFAULT_SUSPENDED_REASON = "Tài khoản đã bị khóa bởi quản trị viên";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final GreenActionPostRepository greenActionPostRepository;
    private final UserManagementActionRepository userManagementActionRepository;
    private final UserProfileMapper userProfileMapper;
    private final NGOProfileMapper ngoProfileMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserAdminSummaryResponseDTO> findAllUsersForAdmin(RoleName role, String search, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                clampPageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<UserEntity> specification = UserSpecification.buildSpecification(role, search);
        Page<UserEntity> usersPage = userRepository.findAll(specification, pageable);

        if (usersPage.isEmpty()) {
            return PagedResponse.of(
                    List.of(),
                    usersPage.getNumber(),
                    usersPage.getSize(),
                    usersPage.getTotalElements(),
                    usersPage.getTotalPages());
        }

        Set<String> userIds = usersPage.getContent().stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
        Map<String, PointWalletEntity> walletsByUserId = pointWalletRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(PointWalletEntity::getUserId, Function.identity()));

        List<UserAdminSummaryResponseDTO> content = usersPage.getContent().stream()
                .map(user -> toAdminSummary(user, walletsByUserId.get(user.getId())))
                .toList();

        return PagedResponse.of(
                content,
                usersPage.getNumber(),
                usersPage.getSize(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminSummaryResponseDTO findUserByIdForAdmin(String id) {
        UserEntity user = userRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        PointWalletEntity wallet = pointWalletRepository.findByUserId(id).orElse(null);
        return toAdminDetail(user, wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponseDTO getCurrentUser() {
        String userId = getCurrentUserId();
        return userRepository.findByIdWithDetails(userId)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    @Override
    @Transactional
    public UserAdminSummaryResponseDTO suspendUser(String userId, SuspendUserRequestDTO request) {
        UserEntity user = getUserOrThrow(userId);
        user.setStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);
        saveManagementAction(user, UserManagementActionType.SUSPEND, request.getReason());
        return findUserByIdForAdmin(userId);
    }

    @Override
    @Transactional
    public UserAdminSummaryResponseDTO changeUserRole(String userId, ChangeUserRoleRequestDTO request) {
        UserEntity user = getUserOrThrow(userId);
        String normalizedRoleName = normalizeRoleName(request.getRoleName());
        Set<RoleEntity> roles = buildRolesForTargetRole(normalizedRoleName);
        user.setRoles(roles);
        userRepository.save(user);
        return findUserByIdForAdmin(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CtvEligibilityResponseDTO checkCurrentUserCtvEligibility() {
        String userId = getCurrentUserId();
        BigDecimal accumulatedPoints = resolveAccumulatedPoints(userId);
        return CtvEligibilityResponseDTO.builder()
                .accumulatedPoints(accumulatedPoints)
                .eligibleForCtv(accumulatedPoints.compareTo(MIN_CTV_ACCUMULATED_POINTS) >= 0)
                .build();
    }

    @Override
    @Transactional
    public UserAdminSummaryResponseDTO upgradeCurrentUserToCtv() {
        String userId = getCurrentUserId();
        UserEntity user = getUserOrThrow(userId);

        if (hasRole(user, ROLE_CTV)) {
            return findUserByIdForAdmin(userId);
        }

        BigDecimal accumulatedPoints = resolveAccumulatedPoints(userId);
        if (accumulatedPoints.compareTo(MIN_CTV_ACCUMULATED_POINTS) < 0) {
            throw new AppException("Bạn cần tối thiểu 100 điểm tích lũy để nâng cấp lên CTV", HttpStatus.BAD_REQUEST);
        }

        RoleEntity userRole = getRoleOrThrow(ROLE_USER);
        RoleEntity ctvRole = getRoleOrThrow(ROLE_CTV);

        user.getRoles().add(userRole);
        user.getRoles().add(ctvRole);
        userRepository.save(user);

        return findUserByIdForAdmin(userId);
    }

    @Override
    @Transactional
    public UserAdminSummaryResponseDTO demoteCtvToUser(String userId, DemoteCtvRequestDTO request) {
        UserEntity user = getUserOrThrow(userId);
        if (!hasRole(user, ROLE_CTV)) {
            throw new AppException("Người dùng không phải tài khoản CTV", HttpStatus.BAD_REQUEST);
        }

        RoleEntity userRole = getRoleOrThrow(ROLE_USER);
        user.getRoles().removeIf(role -> ROLE_CTV.equalsIgnoreCase(role.getName()));
        user.getRoles().add(userRole);

        userRepository.save(user);
        saveManagementAction(user, UserManagementActionType.DEMOTE_CTV, request.getReason());

        return findUserByIdForAdmin(userId);
    }

    private UserAdminSummaryResponseDTO toAdminSummary(UserEntity user, PointWalletEntity wallet) {
        return UserAdminSummaryResponseDTO.builder()
                .id(user.getId())
                .name(resolveName(user))
                .avatarUrl(resolveAvatarUrl(user))
                .createdAt(user.getCreatedAt())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(resolveRoleNames(user))
                .status(user.getStatus())
                .availableGreenPoints(resolveAvailablePoints(user.getId(), wallet))
                .greenPostCount(greenActionPostRepository.countByUser_IdAndIsDeletedFalse(user.getId()))
                .suspensionReason(resolveSuspensionReason(user))
                .build();
    }

    private UserAdminSummaryResponseDTO toAdminDetail(UserEntity user, PointWalletEntity wallet) {
        UserAdminSummaryResponseDTO response = toAdminSummary(user, wallet);

        if (hasRole(user, ROLE_NGO)) {
            response.setNgoProfile(user.getNgoProfile() == null ? null : ngoProfileMapper.toDto(user.getNgoProfile()));
            response.setUserProfile(null);
            return response;
        }

        UserProfileResponseDTO userProfile = user.getUserProfile() == null
                ? null
                : userProfileMapper.toDto(user.getUserProfile());
        response.setUserProfile(userProfile);
        response.setNgoProfile(null);
        return response;
    }

    private BigDecimal resolveAvailablePoints(String userId, PointWalletEntity wallet) {
        if (wallet != null && wallet.getAvailablePoints() != null) {
            return wallet.getAvailablePoints();
        }
        BigDecimal available = pointTransactionRepository.sumAvailablePointsByUserId(userId);
        return available == null ? ZERO : available;
    }

    private BigDecimal resolveAccumulatedPoints(String userId) {
        return pointWalletRepository.findByUserId(userId)
                .map(PointWalletEntity::getTotalPoints)
                .filter(points -> points != null)
                .orElseGet(() -> {
                    BigDecimal accumulated = pointTransactionRepository.sumAccumulatedPointsByUserId(userId);
                    return accumulated == null ? ZERO : accumulated;
                });
    }

    private String resolveSuspensionReason(UserEntity user) {
        if (user.getStatus() != AccountStatus.SUSPENDED) {
            return null;
        }

        return userManagementActionRepository
                .findTopByUser_IdAndActionTypeOrderByCreatedAtDesc(user.getId(), UserManagementActionType.SUSPEND)
                .map(UserManagementActionEntity::getReason)
                .filter(this::hasText)
                .orElse(DEFAULT_SUSPENDED_REASON);
    }

    private String resolveName(UserEntity user) {
        if (user.getUserProfile() != null) {
            if (hasText(user.getUserProfile().getDisplayName())) {
                return user.getUserProfile().getDisplayName();
            }

            String firstName = user.getUserProfile().getFirstName();
            String lastName = user.getUserProfile().getLastName();
            String fullName = (firstName == null ? "" : firstName.trim()) + " "
                    + (lastName == null ? "" : lastName.trim());
            if (hasText(fullName.trim())) {
                return fullName.trim();
            }
        }

        if (hasText(user.getUsername())) {
            return user.getUsername();
        }

        if (hasText(user.getEmail())) {
            return user.getEmail();
        }

        return user.getPhoneNumber();
    }

    private String resolveAvatarUrl(UserEntity user) {
        if (user.getUserProfile() == null || user.getUserProfile().getAvatar() == null) {
            return null;
        }
        return user.getUserProfile().getAvatar().getImageUrl();
    }

    private Set<String> resolveRoleNames(UserEntity user) {
        return user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
    }

    private Set<RoleEntity> buildRolesForTargetRole(String normalizedRoleName) {
        RoleEntity targetRole = getRoleOrThrow(normalizedRoleName);
        RoleEntity userRole = getRoleOrThrow(ROLE_USER);

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);
        if (!ROLE_USER.equals(normalizedRoleName)) {
            roles.add(targetRole);
        }
        return roles;
    }

    private String normalizeRoleName(String roleName) {
        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasRole(UserEntity user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
    }

    private RoleEntity getRoleOrThrow(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role " + roleName + " not found"));
    }

    private UserEntity getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private void saveManagementAction(UserEntity user, UserManagementActionType actionType, String reason) {
        UserManagementActionEntity action = UserManagementActionEntity.builder()
                .user(user)
                .actionType(actionType)
                .reason(reason)
                .actorUserId(getCurrentUserId())
                .build();
        userManagementActionRepository.save(action);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int clampPageSize(int size) {
        return Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
    }
}
