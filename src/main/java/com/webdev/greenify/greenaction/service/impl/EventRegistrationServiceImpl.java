package com.webdev.greenify.greenaction.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.common.exception.TokenException;
import com.webdev.greenify.config.JwtProperties;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventRegistrationMapper registrationMapper;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public EventRegistrationResponseDTO register(EventRegistrationRequestDTO request) {
        String currentUserId = getCurrentUserId();
        
        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() != GreenEventStatus.PUBLISHED) {
            throw new AppException("Event is not open for registration", HttpStatus.BAD_REQUEST);
        }

        if (event.getSignUpDeadlineHoursBefore() != null) {
            LocalDateTime deadline = event.getStartTime().minusHours(event.getSignUpDeadlineHoursBefore());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new AppException("Registration deadline has passed", HttpStatus.BAD_REQUEST);
            }
        }

        boolean alreadyRegistered = registrationRepository.existsByEventIdAndUserIdAndStatusNot(
                event.getId(), currentUserId, RegistrationStatus.CANCELLED);
        if (alreadyRegistered) {
            throw new AppException("You are already registered or waitlisted for this event", HttpStatus.CONFLICT);
        }

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RegistrationStatus targetStatus = RegistrationStatus.REGISTERED;
        long currentParticipants = 0;
        if (event.getMaxParticipants() != null) {
            currentParticipants = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);
            if (currentParticipants >= event.getMaxParticipants()) {
                throw new AppException("Maximum number of participants reached", HttpStatus.BAD_REQUEST);
            }
        }

        String registrationCode = event.getId() + "-" + user.getId() + "-" + System.currentTimeMillis();
        String signedRegistrationCode = signRegistrationCode(registrationCode);

        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .event(event)
                .user(user)
                .status(targetStatus)
                .note(request.getNote())
                .registrationCode(signedRegistrationCode)
                .build();

        registration = registrationRepository.save(registration);
        event.setParticipantCount(currentParticipants + 1);
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

        if (event.getCancelDeadlineHoursBefore() != null) {
            LocalDateTime deadline = event.getStartTime().minusHours(event.getCancelDeadlineHoursBefore());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new AppException("Cancellation deadline has passed (required: " + event.getCancelDeadlineHoursBefore() + "h before)", HttpStatus.BAD_REQUEST);
            }
        }

        RegistrationStatus oldStatus = registration.getStatus();
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
        event.setParticipantCount(event.getParticipantCount() - 1);
        log.info("User {} cancelled registration for event {}", currentUserId, event.getId());

        if (oldStatus == RegistrationStatus.REGISTERED) {
            promoteFromWaitlist(event);
        }
    }

    private void promoteFromWaitlist(EventEntity event) {
        List<EventRegistrationEntity> waitlist = registrationRepository.findTopWaitlistedByEventId(event.getId());
        if (!waitlist.isEmpty()) {
            EventRegistrationEntity firstOnWaitlist = waitlist.getFirst();
            firstOnWaitlist.setStatus(RegistrationStatus.REGISTERED);
            registrationRepository.save(firstOnWaitlist);
            event.setParticipantCount(event.getParticipantCount() + 1);
            log.info("Promoted user {} from waitlist to registered for event {}", firstOnWaitlist.getUser().getId(), event.getId());
        }
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String signRegistrationCode(String registrationCode) {
        try {
            JWSSigner signer = new MACSigner(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(registrationCode)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new TokenException("Error signing registration code", e);
        }
    }
}
