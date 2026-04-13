package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventStatusRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PagedResponse<EventResponseDTO>> getEvents(
            @RequestParam(required = false) GreenEventStatus status,
            @RequestParam(required = false) GreenEventType eventType,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getEventsWithFilter(status, eventType, title, from, to, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
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
}
