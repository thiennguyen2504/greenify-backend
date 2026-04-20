package com.webdev.greenify.greenaction.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
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
import com.webdev.greenify.greenaction.service.PointService;
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
    private final PointService pointService;

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

        boolean alreadyRegistered = registrationRepository.existsByEventIdAndUserIdAndRegistrationStatusNot(
                event.getId(), currentUserId, RegistrationStatus.CANCELLED);
        if (alreadyRegistered) {
            throw new AppException("You are already registered or waitlisted for this event", HttpStatus.CONFLICT);
        }

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RegistrationStatus targetStatus = RegistrationStatus.REGISTERED;
        long currentParticipants = 0;
        if (event.getMaxParticipants() != null) {
            currentParticipants = registrationRepository.countByEventIdAndRegistrationStatus(event.getId(), RegistrationStatus.REGISTERED);
            if (currentParticipants >= event.getMaxParticipants()) {
                throw new AppException("Maximum number of participants reached", HttpStatus.BAD_REQUEST);
            }
        }

        String registrationCode = event.getId() + "-" + user.getId() + "-" + System.currentTimeMillis();
        String signedRegistrationCode = signRegistrationCode(registrationCode);

        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .event(event)
                .user(user)
            .registrationStatus(targetStatus)
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
    public EventRegistrationResponseDTO addToWaitlist(EventRegistrationRequestDTO request) {
        String currentUserId = getCurrentUserId();
        
        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() != GreenEventStatus.PUBLISHED) {
            throw new AppException("Event is not open for registration", HttpStatus.BAD_REQUEST);
        }

        boolean alreadyRegistered = registrationRepository.existsByEventIdAndUserIdAndRegistrationStatusNot(
                event.getId(), currentUserId, RegistrationStatus.CANCELLED);
        if (alreadyRegistered) {
            throw new AppException("You are already registered or waitlisted for this event", HttpStatus.CONFLICT);
        }

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .event(event)
                .user(user)
            .registrationStatus(RegistrationStatus.WAITLISTED)
                .note(request.getNote())
                .build();

        registration = registrationRepository.save(registration);
        log.info("User {} added to waitlist for event {}", currentUserId, event.getId());

        return registrationMapper.toResponse(registration);
    }

    @Override
    @Transactional
    public void checkIn(String registrationCode) {
        decodeRegistrationCode(registrationCode); // Verify token
        EventRegistrationEntity registration = registrationRepository.findByRegistrationCodeAndIsDeletedFalse(registrationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with this code"));

        if (!isEventHappen(registration.getEvent())) {
            throw new AppException("Can only check in during the event time", HttpStatus.BAD_REQUEST);
        }

        if (registration.getRegistrationStatus() != RegistrationStatus.REGISTERED) {
            throw new AppException("Only registered participants can check in", HttpStatus.BAD_REQUEST);
        }

        if (registration.getCheckInTime() != null) {
            throw new AppException("Already checked in", HttpStatus.BAD_REQUEST);
        }

        registration.setCheckInTime(LocalDateTime.now());
        registrationRepository.save(registration);
        log.info("User {} checked in for event {}", registration.getUser().getId(), registration.getEvent().getId());
    }

    @Override
    @Transactional
    public void checkOut(String registrationCode) {
        decodeRegistrationCode(registrationCode); // Verify token
        EventRegistrationEntity registration = registrationRepository.findByRegistrationCodeAndIsDeletedFalse(registrationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with this code"));

        if (registration.getCheckInTime() == null) {
            throw new AppException("Must check in before checking out", HttpStatus.BAD_REQUEST);
        }

        if (registration.getCheckOutTime() != null) {
            throw new AppException("Already checked out", HttpStatus.BAD_REQUEST);
        }

        registration.setCheckOutTime(LocalDateTime.now());
        registrationRepository.save(registration);
        
        // Award points
        EventEntity event = registration.getEvent();
        if (event.getRewardPoints() != null && event.getRewardPoints() > 0) {
            pointService.awardPointsForEventParticipation(registration.getUser(), event);
        }
        
        log.info("User {} checked out for event {} and awarded points", registration.getUser().getId(), event.getId());
    }

    private boolean isEventHappen(EventEntity event) {
        return LocalDateTime.now().isBefore(event.getEndTime()) && LocalDateTime.now().isAfter(event.getStartTime());
    }

    @Override
    @Transactional
    public void cancel(String id) {
        String currentUserId = getCurrentUserId();
        
        EventRegistrationEntity registration = registrationRepository.findByIdAndUserIdAndIsDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found or access denied"));

        if (registration.getRegistrationStatus() == RegistrationStatus.CANCELLED) {
            throw new AppException("Registration is already cancelled", HttpStatus.BAD_REQUEST);
        }

        EventEntity event = registration.getEvent();

        if (event.getCancelDeadlineHoursBefore() != null) {
            LocalDateTime deadline = event.getStartTime().minusHours(event.getCancelDeadlineHoursBefore());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new AppException("Cancellation deadline has passed (required: " + event.getCancelDeadlineHoursBefore() + "h before)", HttpStatus.BAD_REQUEST);
            }
        }

        RegistrationStatus oldStatus = registration.getRegistrationStatus();
        registration.setRegistrationStatus(RegistrationStatus.CANCELLED);
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
            firstOnWaitlist.setRegistrationStatus(RegistrationStatus.REGISTERED);
            
            // Generate registration code for the promoted user
            String code = event.getId() + "-" + firstOnWaitlist.getUser().getId() + "-" + System.currentTimeMillis();
            firstOnWaitlist.setRegistrationCode(signRegistrationCode(code));
            
            registrationRepository.save(firstOnWaitlist);
            event.setParticipantCount(event.getParticipantCount() + 1);
            log.info("Promoted user {} from waitlist to registered for event {}", firstOnWaitlist.getUser().getId(), event.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getRegistrationCode(String eventId, String userId) {
        EventRegistrationEntity registration = registrationRepository.findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        return registration.getRegistrationCode();
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

    private String decodeRegistrationCode(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                throw new AppException("Invalid registration code signature", HttpStatus.BAD_REQUEST);
            }
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new AppException("Invalid registration code", HttpStatus.BAD_REQUEST);
        }
    }
}
