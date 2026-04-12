package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventStatusRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;

import java.time.LocalDateTime;

public interface EventService {
    EventResponseDTO createEvent(EventRequestDTO request);
    PagedResponse<EventResponseDTO> getEventsWithFilter(
            GreenEventStatus status,
            GreenEventType eventType,
            String title,
            LocalDateTime from,
            LocalDateTime to,
            int page, 
            int size);
    EventResponseDTO getEventDetail(String id);
    EventResponseDTO updateEvent(String id, EventRequestDTO request);
    void deleteEvent(String id);
    void approveEvent(String id);
    void rejectEvent(String id, EventStatusRequestDTO request);
}
