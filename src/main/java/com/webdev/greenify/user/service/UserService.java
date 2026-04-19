package com.webdev.greenify.user.service;

import com.webdev.greenify.user.dto.ChangeUserRoleRequestDTO;
import com.webdev.greenify.user.dto.CtvEligibilityResponseDTO;
import com.webdev.greenify.user.dto.DemoteCtvRequestDTO;
import com.webdev.greenify.user.dto.PagedResponse;
import com.webdev.greenify.user.dto.SuspendUserRequestDTO;
import com.webdev.greenify.user.dto.UserAdminSummaryResponseDTO;
import com.webdev.greenify.user.dto.UserDetailResponseDTO;
import com.webdev.greenify.user.enumeration.RoleName;

public interface UserService {
    PagedResponse<UserAdminSummaryResponseDTO> findAllUsersForAdmin(RoleName role, String search, int page, int size);

    UserAdminSummaryResponseDTO findUserByIdForAdmin(String id);

    UserDetailResponseDTO getCurrentUser();

    UserAdminSummaryResponseDTO suspendUser(String userId, SuspendUserRequestDTO request);

    UserAdminSummaryResponseDTO changeUserRole(String userId, ChangeUserRoleRequestDTO request);

    CtvEligibilityResponseDTO checkCurrentUserCtvEligibility();

    UserAdminSummaryResponseDTO upgradeCurrentUserToCtv();

    UserAdminSummaryResponseDTO demoteCtvToUser(String userId, DemoteCtvRequestDTO request);
}
