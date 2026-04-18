package com.webdev.greenify.user.service;

import com.webdev.greenify.user.dto.NGOProfileFilterRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileRejectRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileResponseDTO;
import com.webdev.greenify.user.dto.PagedResponse;

public interface NGOProfileService {
    NGOProfileResponseDTO createNGOProfile(NGOProfileRequestDTO request);
    NGOProfileResponseDTO updateNGOProfile(NGOProfileRequestDTO request);
    NGOProfileResponseDTO approveNGOProfile(String id);
    NGOProfileResponseDTO rejectNGOProfile(String id, NGOProfileRejectRequestDTO request);
    NGOProfileResponseDTO getCurrentNGOProfile();
    PagedResponse<NGOProfileResponseDTO> searchNGOProfiles(NGOProfileFilterRequestDTO filter);
    NGOProfileResponseDTO getNGOProfileById(String id);
}
