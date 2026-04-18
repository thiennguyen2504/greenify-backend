package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventStatusRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.webdev.greenify.greenaction.dto.request.EventPredictionRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventPredictionResponseDTO;

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
    
    List<EventRegistrationResponseDTO> getEventRegistrations(String eventId);
    
    PagedResponse<EventResponseDTO> getParticipatedEvents(
            String userId,
            String title,
            RegistrationStatus status,
            String address,
            int page,
            int size);

    EventPredictionResponseDTO predictEventFeasibility(EventPredictionRequestDTO request);
}
