package com.webdev.greenify.user.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.file.entity.ProfileImageEntity;
import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.file.repository.ProfileImageRepository;
import com.webdev.greenify.user.dto.UserProfileCreateRequestDTO;
import com.webdev.greenify.user.dto.UserProfileResponseDTO;
import com.webdev.greenify.user.dto.UserProfileUpdateRequestDTO;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.entity.UserProfileEntity;
import com.webdev.greenify.user.enumeration.ImageStatus;
import com.webdev.greenify.user.enumeration.UserProfileStatus;
import com.webdev.greenify.user.mapper.UserProfileMapper;
import com.webdev.greenify.user.repository.UserProfileRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final ProfileImageRepository profileImageRepository;
    private final ImageMapper imageMapper;

    @Override
    @Transactional
    public UserProfileResponseDTO getCurrentUserProfile() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        return userProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO createCurrentUserProfile(UserProfileCreateRequestDTO request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userProfileRepository.findByUserId(userId).ifPresent(p -> {
            throw new AppException("User profile already exists", HttpStatus.BAD_REQUEST);
        });

        UserProfileEntity profile = userProfileMapper.toEntity(request);
        profile.setUser(user);
        profile.setStatus(isProfileComplete(profile) ? UserProfileStatus.COMPLETE : UserProfileStatus.IN_COMPLETE);

        if (request.getAvatar() != null) {
            ProfileImageEntity image = imageMapper.toProfileImageEntity(request.getAvatar());
            image.setStatus(ImageStatus.ACTIVE);
            image.setUserProfile(profile);
            profile.setAvatar(image);
        }
        UserProfileEntity savedUserProfile = userProfileRepository.save(profile);
        return userProfileMapper.toDto(savedUserProfile);
    }

    private Boolean isProfileComplete(UserProfileEntity user) {
        return user.getDisplayName() != null
                && user.getProvince() != null && user.getWard() != null;
    }

    @Override
    @Transactional
    public UserProfileResponseDTO updateCurrentUserProfile(UserProfileUpdateRequestDTO request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        userProfileMapper.updateProfileFromDto(request, profile);
        if (request.getAvatar() != null) {
            if (profile.getAvatar() != null)
                imageMapper.updateProfileImage(request.getAvatar(), profile.getAvatar());
            else {
                ProfileImageEntity image = imageMapper.toProfileImageEntity(request.getAvatar());
                image.setStatus(ImageStatus.ACTIVE);
                image.setUserProfile(profile);
                profile.setAvatar(image);
            }
        }
        profile.setStatus(isProfileComplete(profile) ? UserProfileStatus.COMPLETE : UserProfileStatus.IN_COMPLETE);
        return userProfileMapper.toDto(userProfileRepository.save(profile));
    }
}
