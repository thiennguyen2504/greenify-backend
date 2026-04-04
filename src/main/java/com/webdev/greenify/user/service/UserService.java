package com.webdev.greenify.user.service;

import com.webdev.greenify.user.dto.UpdateUserRolesRequestDTO;
import com.webdev.greenify.user.dto.UserDetailResponseDTO;

import java.util.List;

public interface UserService {
    List<UserDetailResponseDTO> findAllUsers();
    UserDetailResponseDTO findUserById(String id);
    UserDetailResponseDTO getCurrentUser();
    UserDetailResponseDTO updateUserRoles(String userId, UpdateUserRolesRequestDTO request);
}
