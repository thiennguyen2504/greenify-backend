package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.EventRegistrationRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;

public interface EventRegistrationService {
    EventRegistrationResponseDTO register(EventRegistrationRequestDTO request);
    EventRegistrationResponseDTO addToWaitlist(EventRegistrationRequestDTO request);
    void checkIn(String registrationCode);
    void checkOut(String registrationCode);
    void cancel(String id);
    String getRegistrationCode(String eventId, String userId);
}
