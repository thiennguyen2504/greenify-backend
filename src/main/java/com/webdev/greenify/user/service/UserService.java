package com.webdev.greenify.user.service;

import com.webdev.greenify.user.dto.ChangeUserRoleRequestDTO;
import com.webdev.greenify.user.dto.CtvEligibilityResponseDTO;
import com.webdev.greenify.user.dto.DemoteCtvRequestDTO;
import com.webdev.greenify.user.dto.SuspendUserRequestDTO;
import com.webdev.greenify.user.dto.UserAdminSummaryResponseDTO;
import com.webdev.greenify.user.dto.UserDetailResponseDTO;

import java.util.List;

public interface UserService {
    List<UserAdminSummaryResponseDTO> findAllUsersForAdmin();

    UserAdminSummaryResponseDTO findUserByIdForAdmin(String id);

    UserDetailResponseDTO getCurrentUser();

    UserAdminSummaryResponseDTO suspendUser(String userId, SuspendUserRequestDTO request);

    UserAdminSummaryResponseDTO changeUserRole(String userId, ChangeUserRoleRequestDTO request);

    CtvEligibilityResponseDTO checkCurrentUserCtvEligibility();

    UserAdminSummaryResponseDTO upgradeCurrentUserToCtv();

    UserAdminSummaryResponseDTO demoteCtvToUser(String userId, DemoteCtvRequestDTO request);
}
