package com.webdev.greenify.user.service.impl;

import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.user.dto.UserDetailResponseDTO;
import com.webdev.greenify.user.mapper.UserMapper;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;

    @Override
    public List<UserDetailResponseDTO> findAllUsers() {
        return repository.findAllWithRoles().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDetailResponseDTO findUserById(String id) {
        return repository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("UserEntity not found"));
    }
}
