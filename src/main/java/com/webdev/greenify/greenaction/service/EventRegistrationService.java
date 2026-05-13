package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.EventRegistrationRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;

public interface EventRegistrationService {
    EventRegistrationResponseDTO register(EventRegistrationRequestDTO request);
    EventRegistrationResponseDTO addToWaitlist(EventRegistrationRequestDTO request);
    void checkIn(String registrationCode, Double latitude, Double longitude);
    void checkOut(String registrationCode, Double latitude, Double longitude);
    void cancel(String id);
    String getRegistrationCode(String eventId, String userId);
}
