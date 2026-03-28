package com.webdev.greenify.service;

import com.webdev.greenify.dto.UserDetailResponseDTO;

import java.util.List;

public interface UserService {
    List<UserDetailResponseDTO> findAllUsers();
    UserDetailResponseDTO findUserById(String id);
}
