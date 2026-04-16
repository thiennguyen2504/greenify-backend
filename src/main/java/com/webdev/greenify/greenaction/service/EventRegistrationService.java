package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.EventRegistrationRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;

public interface EventRegistrationService {
    EventRegistrationResponseDTO register(EventRegistrationRequestDTO request);
    void cancel(String id);
}
