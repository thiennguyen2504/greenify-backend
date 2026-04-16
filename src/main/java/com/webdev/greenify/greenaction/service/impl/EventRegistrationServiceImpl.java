package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.greenaction.dto.request.EventRegistrationRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import com.webdev.greenify.greenaction.mapper.EventRegistrationMapper;
import com.webdev.greenify.greenaction.repository.EventRegistrationRepository;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.greenaction.service.EventRegistrationService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventRegistrationMapper registrationMapper;

    @Override
    @Transactional
    public EventRegistrationResponseDTO register(EventRegistrationRequestDTO request) {
        String currentUserId = getCurrentUserId();
        
        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // 1. Validate Event Status
        if (event.getStatus() != GreenEventStatus.PUBLISHED) {
            throw new AppException("Event is not open for registration", HttpStatus.BAD_REQUEST);
        }

        // 2. Validate Sign Up Deadline
        if (event.getSignUpDeadlineHoursBefore() != null) {
            LocalDateTime deadline = event.getStartTime().minusHours(event.getSignUpDeadlineHoursBefore());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new AppException("Registration deadline has passed", HttpStatus.BAD_REQUEST);
            }
        }

        // 3. Check if user already registered (not cancelled)
        boolean alreadyRegistered = registrationRepository.existsByEventIdAndUserIdAndStatusNot(
                event.getId(), currentUserId, RegistrationStatus.CANCELLED);
        if (alreadyRegistered) {
            throw new AppException("You are already registered or waitlisted for this event", HttpStatus.CONFLICT);
        }

        // 4. Get Current User
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 5. Determine Status (Registered or Waitlisted)
        RegistrationStatus targetStatus = RegistrationStatus.REGISTERED;
        if (event.getMaxParticipants() != null) {
            long currentParticipants = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);
            if (currentParticipants >= event.getMaxParticipants()) {
                targetStatus = RegistrationStatus.WAITLISTED;
            }
        }

        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .event(event)
                .user(user)
                .status(targetStatus)
                .note(request.getNote())
                .build();

        registration = registrationRepository.save(registration);
        log.info("User {} registered for event {} with status {}", currentUserId, event.getId(), targetStatus);

        return registrationMapper.toResponse(registration);
    }

    @Override
    @Transactional
    public void cancel(String id) {
        String currentUserId = getCurrentUserId();
        
        EventRegistrationEntity registration = registrationRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found or access denied"));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new AppException("Registration is already cancelled", HttpStatus.BAD_REQUEST);
        }

        EventEntity event = registration.getEvent();

        // 1. Validate Cancellation Deadline
        if (event.getCancelDeadlineHoursBefore() != null) {
            LocalDateTime deadline = event.getStartTime().minusHours(event.getCancelDeadlineHoursBefore());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new AppException("Cancellation deadline has passed (required: " + event.getCancelDeadlineHoursBefore() + "h before)", HttpStatus.BAD_REQUEST);
            }
        }

        RegistrationStatus oldStatus = registration.getStatus();
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
        log.info("User {} cancelled registration for event {}", currentUserId, event.getId());

        // 2. Auto-fill from Waitlist if a REGISTERED spot opened up
        if (oldStatus == RegistrationStatus.REGISTERED) {
            promoteFromWaitlist(event.getId());
        }
    }

    private void promoteFromWaitlist(String eventId) {
        List<EventRegistrationEntity> waitlist = registrationRepository.findTopWaitlistedByEventId(eventId);
        if (!waitlist.isEmpty()) {
            EventRegistrationEntity firstOnWaitlist = waitlist.get(0);
            firstOnWaitlist.setStatus(RegistrationStatus.REGISTERED);
            registrationRepository.save(firstOnWaitlist);
            log.info("Promoted user {} from waitlist to registered for event {}", firstOnWaitlist.getUser().getId(), eventId);
        }
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
