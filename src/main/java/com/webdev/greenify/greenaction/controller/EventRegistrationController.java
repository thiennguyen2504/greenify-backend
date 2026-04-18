package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.request.EventRegistrationRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;
import com.webdev.greenify.greenaction.service.EventRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/event-registrations")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<EventRegistrationResponseDTO> register(
            @Valid @RequestBody EventRegistrationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @PostMapping("/waitlist")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<EventRegistrationResponseDTO> addToWaitlist(
            @Valid @RequestBody EventRegistrationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.addToWaitlist(request));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<Void> checkIn(@RequestParam String code) {
        registrationService.checkIn(code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<Void> checkOut(@RequestParam String code) {
        registrationService.checkOut(code);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/code")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<String> getRegistrationCode(
            @RequestParam String eventId,
            @RequestParam String userId) {
        return ResponseEntity.ok(registrationService.getRegistrationCode(eventId, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
