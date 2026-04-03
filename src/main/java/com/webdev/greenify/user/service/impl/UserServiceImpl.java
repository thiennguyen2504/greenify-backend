package com.webdev.greenify.user.service.impl;

import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.user.dto.UpdateUserRolesRequestDTO;
import com.webdev.greenify.user.dto.UserDetailResponseDTO;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.mapper.UserMapper;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    @Override
    public List<UserDetailResponseDTO> findAllUsers() {
        return repository.findAllWithRoles().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public UserDetailResponseDTO findUserById(String id) {
        return repository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public UserDetailResponseDTO getCurrentUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return findUserById(userId);
    }

    @Override
    @Transactional
    public UserDetailResponseDTO updateUserRoles(String userId, UpdateUserRolesRequestDTO request) {
        UserEntity user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<RoleEntity> roles = roleRepository.findAllByNames(request.getRoleNames());
        user.getRoles().addAll(roles);
        return userMapper.toDto(repository.save(user));
    }
}
