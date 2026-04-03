package com.webdev.greenify.user.service;

import com.webdev.greenify.user.dto.UserProfileCreateRequestDTO;
import com.webdev.greenify.user.dto.UserProfileResponseDTO;
import com.webdev.greenify.user.dto.UserProfileUpdateRequestDTO;

public interface ProfileService {
    UserProfileResponseDTO getCurrentUserProfile();
    UserProfileResponseDTO createCurrentUserProfile(UserProfileCreateRequestDTO request);
    UserProfileResponseDTO updateCurrentUserProfile(UserProfileUpdateRequestDTO request);
}
