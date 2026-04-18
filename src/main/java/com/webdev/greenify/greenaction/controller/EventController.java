package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.request.EventPredictionRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventStatusRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventPredictionResponseDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import com.webdev.greenify.greenaction.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGO')")
    public ResponseEntity<EventResponseDTO> createEvent(@Valid @RequestBody EventRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    // Admin API: Management and full visibility
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<EventResponseDTO>> getEventsForAdmin(
            @RequestParam(required = false) List<GreenEventStatus> statuses,
            @RequestParam(required = false) GreenEventType eventType,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String organizerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getEventsForAdmin(statuses, eventType, title, from, to, organizerId, page, size));
    }

    // Public API: Only published events
    @GetMapping("/public")
    public ResponseEntity<PagedResponse<EventResponseDTO>> getEventsForPublic(
            @RequestParam(required = false) GreenEventType eventType,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getEventsForPublic(eventType, title, from, to, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getEventDetail(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<EventResponseDTO> updateEvent(
            @PathVariable String id, 
            @Valid @RequestBody EventRequestDTO request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<Void> submitEvent(@PathVariable String id) {
        eventService.submitEvent(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveEvent(@PathVariable String id) {
        eventService.approveEvent(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectEvent(
            @PathVariable String id, 
            @Valid @RequestBody EventStatusRequestDTO request) {
        eventService.rejectEvent(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<PagedResponse<EventResponseDTO>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getMyEvents(page, size));
    }

    @GetMapping("/ngo/{ngoId}")
    public ResponseEntity<PagedResponse<EventResponseDTO>> getNGOEventsPublic(
            @PathVariable String ngoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getNGOEventsPublic(ngoId, page, size));
    }

    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGO')")
    public ResponseEntity<List<EventRegistrationResponseDTO>> getEventRegistrations(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventRegistrations(id));
    }

    @GetMapping("/participated/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
    public ResponseEntity<PagedResponse<EventResponseDTO>> getParticipatedEvents(
            @PathVariable String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) String address,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getParticipatedEvents(userId, title, status, address, page, size));
    }

    @PostMapping("/predict")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    public ResponseEntity<EventPredictionResponseDTO> predictEventFeasibility(@Valid @RequestBody EventPredictionRequestDTO request) {
        return ResponseEntity.ok(eventService.predictEventFeasibility(request));
    }
}
