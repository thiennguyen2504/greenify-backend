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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));

        if (event.getStatus() != GreenEventStatus.PUBLISHED) {
            throw new AppException("Sự kiện hiện không mở đăng ký", HttpStatus.BAD_REQUEST);
        }

        if (event.getSignUpDeadlineHoursBefore() != null) {
            LocalDateTime deadline = event.getStartTime().minusHours(event.getSignUpDeadlineHoursBefore());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new AppException("Đã quá hạn đăng ký", HttpStatus.BAD_REQUEST);
            }
        }

        boolean alreadyRegistered = registrationRepository.existsByEventIdAndUserIdAndRegistrationStatusNot(
                event.getId(), currentUserId, RegistrationStatus.CANCELLED);
        if (alreadyRegistered) {
            throw new AppException("Bạn đã đăng ký hoặc đang trong danh sách chờ của sự kiện này", HttpStatus.CONFLICT);
        }

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        RegistrationStatus targetStatus = RegistrationStatus.REGISTERED;
        long currentParticipants = 0;
        if (event.getMaxParticipants() != null) {
            currentParticipants = registrationRepository.countByEventIdAndRegistrationStatus(event.getId(), RegistrationStatus.REGISTERED);
            if (currentParticipants >= event.getMaxParticipants()) {
                throw new AppException("Đã đạt số lượng người tham gia tối đa", HttpStatus.BAD_REQUEST);
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));

        if (event.getStatus() != GreenEventStatus.PUBLISHED) {
            throw new AppException("Sự kiện hiện không mở đăng ký", HttpStatus.BAD_REQUEST);
        }

        boolean alreadyRegistered = registrationRepository.existsByEventIdAndUserIdAndRegistrationStatusNot(
                event.getId(), currentUserId, RegistrationStatus.CANCELLED);
        if (alreadyRegistered) {
            throw new AppException("Bạn đã đăng ký hoặc đang trong danh sách chờ của sự kiện này", HttpStatus.CONFLICT);
        }

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký với mã này"));

        if (!isEventHappen(registration.getEvent())) {
            throw new AppException("Chỉ có thể check-in trong thời gian diễn ra sự kiện", HttpStatus.BAD_REQUEST);
        }

        if (registration.getRegistrationStatus() != RegistrationStatus.REGISTERED) {
            throw new AppException("Chỉ người đã đăng ký mới có thể check-in", HttpStatus.BAD_REQUEST);
        }

        if (registration.getCheckInTime() != null) {
            throw new AppException("Bạn đã check-in trước đó", HttpStatus.BAD_REQUEST);
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký với mã này"));

        if (registration.getCheckInTime() == null) {
            throw new AppException("Bạn phải check-in trước khi check-out", HttpStatus.BAD_REQUEST);
        }

        if (registration.getCheckOutTime() != null) {
            throw new AppException("Bạn đã check-out trước đó", HttpStatus.BAD_REQUEST);
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký hoặc bạn không có quyền truy cập"));

        if (registration.getRegistrationStatus() == RegistrationStatus.CANCELLED) {
            throw new AppException("Đăng ký này đã bị hủy", HttpStatus.BAD_REQUEST);
        }

        EventEntity event = registration.getEvent();

        if (event.getCancelDeadlineHoursBefore() != null) {
            LocalDateTime deadline = event.getStartTime().minusHours(event.getCancelDeadlineHoursBefore());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new AppException("Đã quá hạn hủy đăng ký (yêu cầu: " + event.getCancelDeadlineHoursBefore() + " giờ trước)", HttpStatus.BAD_REQUEST);
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
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
            throw new TokenException("Lỗi ký mã đăng ký", e);
        }
    }

    private String decodeRegistrationCode(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                throw new AppException("Chữ ký mã đăng ký không hợp lệ", HttpStatus.BAD_REQUEST);
            }
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new AppException("Mã đăng ký không hợp lệ", HttpStatus.BAD_REQUEST);
        }
    }
}
