package com.webdev.greenify.user.service;

import com.webdev.greenify.user.dto.UserDetailResponseDTO;

import java.util.List;

public interface UserService {
    List<UserDetailResponseDTO> findAllUsers();
    UserDetailResponseDTO findUserById(String id);
    UserDetailResponseDTO getCurrentUser();
}
