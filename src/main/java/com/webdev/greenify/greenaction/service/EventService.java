package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventStatusRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;

import java.time.LocalDateTime;
import java.util.Collection;

public interface EventService {
    EventResponseDTO createEvent(EventRequestDTO request);
    
    // For Admin: Sees everything
    PagedResponse<EventResponseDTO> getEventsForAdmin(
            Collection<GreenEventStatus> statuses,
            GreenEventType eventType,
            String title,
            LocalDateTime from,
            LocalDateTime to,
            String organizerId,
            int page, 
            int size);

    // For Public: Sees only public statuses
    PagedResponse<EventResponseDTO> getEventsForPublic(
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
    void submitEvent(String id);
    PagedResponse<EventResponseDTO> getMyEvents(int page, int size);
    PagedResponse<EventResponseDTO> getNGOEventsPublic(String ngoId, int page, int size);
}
