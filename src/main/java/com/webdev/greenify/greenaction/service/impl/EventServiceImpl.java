package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.common.util.TimeUtils;
import com.webdev.greenify.file.entity.EventImageEntity;
import com.webdev.greenify.file.enumeration.EventImageType;
import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.greenaction.dto.request.EventPredictionRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventStatusRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventParticipationSummaryResponseDTO;
import com.webdev.greenify.greenaction.dto.response.EventPredictionResponseDTO;
import com.webdev.greenify.greenaction.dto.response.EventRegistrationResponseDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.greenaction.enumeration.EventFeasibilityConclusion;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import com.webdev.greenify.greenaction.mapper.EventMapper;
import com.webdev.greenify.greenaction.mapper.EventRegistrationMapper;
import com.webdev.greenify.greenaction.repository.EventRegistrationRepository;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.greenaction.service.EventService;
import com.webdev.greenify.greenaction.specification.EventRegistrationSpecification;
import com.webdev.greenify.greenaction.specification.EventSpecification;
import com.webdev.greenify.notification.enumeration.NotificationType;
import com.webdev.greenify.notification.event.NotificationEvent;
import com.webdev.greenify.station.service.ProvinceNormalizationService;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final ImageMapper imageMapper;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventRegistrationMapper eventRegistrationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ProvinceNormalizationService provinceNormalizationService;

    private static final Set<GreenEventStatus> PUBLIC_STATUSES = EnumSet.of(
            GreenEventStatus.PUBLISHED,
            GreenEventStatus.IN_PROGRESS,
            GreenEventStatus.COMPLETED,
            GreenEventStatus.CANCELLED
    );

    private static final Set<GreenEventStatus> SENSITIVE_STATUSES = EnumSet.of(
            GreenEventStatus.DRAFT,
            GreenEventStatus.APPROVAL_WAITING,
            GreenEventStatus.REJECTED
    );

    @Override
    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        String currentUserId = getCurrentUserId();
        UserEntity organizer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        
        EventEntity event = eventMapper.toEntity(request);
        event.setRejectedCount(0);
        event.setParticipantCount(0L);
        event.setOrganizer(organizer);

        if (event.getStatus() == null) {
            event.setStatus(GreenEventStatus.DRAFT);
        }

        prepareEventRelationships(event, request);

        event = eventRepository.save(event);
        log.info("Event created with ID: {} by user: {}", event.getId(), currentUserId);

        // Publish notification for event creation if it's not a draft
        if (event.getStatus() != GreenEventStatus.DRAFT) {
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    currentUserId,
                    "Tạo sự kiện thành công",
                    "Sự kiện '" + event.getTitle() + "' của bạn đã được tạo thành công và đang chờ duyệt.",
                    NotificationType.EVENT_CREATED_SUCCESS,
                    event.getId()
            ));
        }

        return enrichEventResponseWithRegistrationStatus(eventMapper.toResponse(event), event.getId(), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponseDTO> getEventsForAdmin(
            Collection<GreenEventStatus> statuses,
            GreenEventType eventType,
            String title,
            LocalDateTime from,
            LocalDateTime to,
            String organizerId,
            int page,
            int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<EventEntity> spec = EventSpecification.buildSpecification(statuses, eventType, title, from, to, organizerId);
        return toPagedResponse(eventRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponseDTO> getEventsForPublic(
            GreenEventType eventType,
            String title,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<EventEntity> spec = EventSpecification.buildSpecification(PUBLIC_STATUSES, eventType, title, from, to, null);
        return toPagedResponse(eventRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDTO getEventDetail(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));
        
        if (event.isDeleted()) {
            throw new ResourceNotFoundException("Không tìm thấy sự kiện");
        }

        if (isPublicUser() && SENSITIVE_STATUSES.contains(event.getStatus())) {
            throw new ResourceNotFoundException("Không tìm thấy sự kiện");
        }

        return enrichEventResponseWithRegistrationStatus(
            eventMapper.toResponse(event),
            event.getId(),
            tryGetCurrentUserId());
    }

    @Override
    @Transactional
    public EventResponseDTO updateEvent(String id, EventRequestDTO request) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));

        if (event.isDeleted()) {
            throw new ResourceNotFoundException("Không tìm thấy sự kiện");
        }

        EnumSet<GreenEventStatus> allowableStatuses = EnumSet.of(
                GreenEventStatus.DRAFT, 
                GreenEventStatus.APPROVAL_WAITING, 
                GreenEventStatus.REJECTED);
        
        if (!allowableStatuses.contains(event.getStatus())) {
            throw new AppException("Không thể cập nhật sự kiện ở trạng thái hiện tại: " + event.getStatus(), HttpStatus.BAD_REQUEST);
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());
        eventMapper.updateEvent(request, event);
        prepareEventRelationships(event, request);
        event.setStatus(GreenEventStatus.APPROVAL_WAITING);

        event = eventRepository.save(event);
        log.info("Event updated with ID: {}", event.getId());

        return enrichEventResponseWithRegistrationStatus(
            eventMapper.toResponse(event),
            event.getId(),
            tryGetCurrentUserId());
    }

    @Override
    @Transactional
    public void deleteEvent(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));
        
        event.setDeleted(true);
        eventRepository.save(event);
        log.info("Event soft-deleted with ID: {}", id);
    }

    @Override
    @Transactional
    public void approveEvent(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));
        
        if (event.getStatus() != GreenEventStatus.APPROVAL_WAITING) {
            throw new AppException("Sự kiện không ở trạng thái chờ duyệt", HttpStatus.BAD_REQUEST);
        }

        event.setStatus(GreenEventStatus.PUBLISHED);
        event.setRejectReason(null);
        eventRepository.save(event);
        log.info("Event approved with ID: {}", id);

        // Publish notification
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                event.getOrganizer().getId(),
                "Sự kiện đã được duyệt",
                "Sự kiện '" + event.getTitle() + "' của bạn đã được phê duyệt.",
                NotificationType.EVENT_APPROVED,
                event.getId()
        ));
    }

    @Override
    @Transactional
    public void rejectEvent(String id, EventStatusRequestDTO request) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));
        
        if (event.getStatus() != GreenEventStatus.APPROVAL_WAITING) {
            throw new AppException("Sự kiện không ở trạng thái chờ duyệt", HttpStatus.BAD_REQUEST);
        }

        event.setStatus(GreenEventStatus.REJECTED);
        event.setRejectReason(request.getReason());
        Integer newRejectedCount = event.getRejectedCount() != null ? event.getRejectedCount() + 1 : 1;
        event.setRejectedCount(newRejectedCount);
        eventRepository.save(event);
        log.info("Event rejected with ID: {} Reason: {}", id, request.getReason());

        // Publish notification
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                event.getOrganizer().getId(),
                "Sự kiện bị từ chối",
                "Sự kiện '" + event.getTitle() + "' của bạn đã bị từ chối. Lý do: " + request.getReason(),
                NotificationType.EVENT_REJECTED,
                event.getId()
        ));
    }

    @Override
    @Transactional
    public void submitEvent(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));
        
        if (event.getStatus() != GreenEventStatus.DRAFT && event.getStatus() != GreenEventStatus.REJECTED) {
            throw new AppException("Chỉ sự kiện ở trạng thái DRAFT hoặc REJECTED mới có thể gửi duyệt", HttpStatus.BAD_REQUEST);
        }

        event.setStatus(GreenEventStatus.APPROVAL_WAITING);
        eventRepository.save(event);
        log.info("Event submitted for approval: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponseDTO> getMyEvents(
            GreenEventStatus status,
            GreenEventType eventType,
            String title,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {
        String currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Collection<GreenEventStatus> statuses = status == null ? null : List.of(status);
        Specification<EventEntity> spec = EventSpecification.buildSpecification(statuses, eventType, title, from, to, currentUserId);
        return toPagedResponse(eventRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponseDTO> getNGOEventsPublic(String ngoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<EventEntity> spec = EventSpecification.buildSpecification(PUBLIC_STATUSES, null, null, null, null, ngoId);
        return toPagedResponse(eventRepository.findAll(spec, pageable));
    }

    private PagedResponse<EventResponseDTO> toPagedResponse(Page<EventEntity> eventPage) {
        List<EventResponseDTO> content = mapEventResponsesWithRegistrationStatus(eventPage.getContent());

        return PagedResponse.of(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages());
    }

    private boolean isPublicUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return true;
        
        boolean hasPrivilegedRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_NGO"));
        
        return !hasPrivilegedRole;
    }

    private void prepareEventRelationships(EventEntity event, EventRequestDTO request) {
        if (event.getAddress() != null) {
            event.getAddress().setEvent(event);
            event.getAddress().setProvince(
                    provinceNormalizationService.normalizeProvinceName(event.getAddress().getProvince()));
        }

        List<EventImageEntity> eventImages = event.getImages();
        if (eventImages == null) {
            eventImages = new ArrayList<>();
            event.setImages(eventImages);
        } else {
            try {
                EventImageEntity temp = new EventImageEntity();
                eventImages.add(temp);
                eventImages.remove(temp);
            } catch (UnsupportedOperationException e) {
                eventImages = new ArrayList<>(eventImages);
                event.setImages(eventImages);
            }
        }

        eventImages.forEach(image -> {
            image.setEvent(event);
            image.setImageType(EventImageType.DETAIL);
        });

        if (request.getThumbnail() != null) {
            EventImageEntity thumbnail = imageMapper.toEventImageEntity(request.getThumbnail());
            thumbnail.setEvent(event);
            thumbnail.setImageType(EventImageType.THUMBNAIL);
            eventImages.add(thumbnail);
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!TimeUtils.isValidTime(start, end)) {
            throw new AppException("Thời gian bắt đầu phải trước thời gian kết thúc", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponseDTO> getEventRegistrations(String eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Không tìm thấy sự kiện");
        }
        
        return eventRegistrationRepository.findAllByEventId(eventId).stream()
                .map(eventRegistrationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponseDTO> getParticipatedEvents(
            String userId,
            String title,
            RegistrationStatus registrationStatus,
            String address,
            int page,
            int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<EventRegistrationEntity> spec = EventRegistrationSpecification
                .buildSpecification(userId, title, registrationStatus, address);
        
        Page<EventRegistrationEntity> registrationPage = eventRegistrationRepository.findAll(spec, pageable);
        
        List<EventResponseDTO> content = registrationPage.getContent().stream()
                .map(registration -> {
                    EventResponseDTO response = eventMapper.toResponse(registration.getEvent());
                    response.setRegistrationStatus(registration.getRegistrationStatus());
                    return response;
                })
                .toList();

        return PagedResponse.of(
                content,
                registrationPage.getNumber(),
                registrationPage.getSize(),
                registrationPage.getTotalElements(),
                registrationPage.getTotalPages());
    }

            @Override
            @Transactional(readOnly = true)
            public EventParticipationSummaryResponseDTO getMyParticipationSummary() {
            String currentUserId = getCurrentUserId();

            long registeredCount = eventRegistrationRepository
                .countByUserIdAndRegistrationStatusAndCheckInTimeIsNull(currentUserId, RegistrationStatus.REGISTERED);
            long waitlistedCount = eventRegistrationRepository
                .countByUserIdAndRegistrationStatus(currentUserId, RegistrationStatus.WAITLISTED);
            long cancelledCount = eventRegistrationRepository
                .countByUserIdAndRegistrationStatus(currentUserId, RegistrationStatus.CANCELLED);
            long attendedCount = eventRegistrationRepository
                .countByUserIdAndRegistrationStatus(currentUserId, RegistrationStatus.ATTENDED);

            return EventParticipationSummaryResponseDTO.builder()
                .registeredCount(registeredCount)
                .waitlistedCount(waitlistedCount)
                .cancelledCount(cancelledCount)
                .attendedCount(attendedCount)
                .build();
            }

            @Override
            @Transactional(readOnly = true)
            public PagedResponse<EventResponseDTO> getMyParticipatedEvents(
                String title,
                RegistrationStatus registrationStatus,
                int page,
                int size) {
            String currentUserId = getCurrentUserId();
            return getParticipatedEvents(currentUserId, title, registrationStatus, null, page, size);
            }

    @Override
    @Transactional(readOnly = true)
    public EventPredictionResponseDTO predictEventFeasibility(EventPredictionRequestDTO request) {
        int startHour = request.getStartTime().getHour();
        int endHour = request.getEndTime().getHour();
        String normalizedProvince = provinceNormalizationService.normalizeProvinceName(request.getProvince());
        Double averageParticipants = eventRepository.getAverageParticipantsByCriteria(
                request.getEventType(),
            normalizedProvince,
                startHour,
                endHour);

        double minRatio = 0.0;
        if (request.getMinParticipants() > 0) {
            minRatio = (averageParticipants / request.getMinParticipants()) * 100;
        }

        double expectedRatio = 0.0;
        if (request.getExpectedParticipants() > 0) {
            expectedRatio = (averageParticipants / request.getExpectedParticipants()) * 100;
        }

        EventFeasibilityConclusion conclusion;
        String message;

        if (averageParticipants >= request.getExpectedParticipants()) {
            conclusion = EventFeasibilityConclusion.HIGHLY_FEASIBLE;
            message = String.format("Dựa trên lịch sử, các sự kiện tương tự có trung bình %.1f người tham gia, vượt mức dự kiến của bạn.", averageParticipants);
        } else if (averageParticipants >= request.getMinParticipants()) {
            conclusion = EventFeasibilityConclusion.FEASIBLE;
            message = String.format("Dựa trên lịch sử, các sự kiện tương tự có trung bình %.1f người tham gia, đạt mức tối thiểu đề ra.", averageParticipants);
        } else if (averageParticipants > 0) {
            conclusion = EventFeasibilityConclusion.RISKY;
            message = String.format("Dựa trên lịch sử, các sự kiện tương tự chỉ đạt trung bình %.1f người tham gia, thấp hơn mức tối thiểu.", averageParticipants);
        } else {
            conclusion = EventFeasibilityConclusion.NO_DATA;
            message = "Hiện chưa có dữ liệu quá khứ cho loại sự kiện này tại tỉnh thành này trong khung giờ đã chọn.";
        }

        return EventPredictionResponseDTO.builder()
                .averageParticipants(averageParticipants)
                .minRequirementRatio(minRatio)
                .expectedRequirementRatio(expectedRatio)
                .conclusion(conclusion)
                .message(message)
                .build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private List<EventResponseDTO> mapEventResponsesWithRegistrationStatus(List<EventEntity> events) {
        List<EventResponseDTO> responses = events.stream()
                .map(eventMapper::toResponse)
                .toList();

        String currentUserId = tryGetCurrentUserId();
        if (currentUserId == null || responses.isEmpty()) {
            return responses;
        }

        List<String> eventIds = responses.stream()
                .map(EventResponseDTO::getId)
                .toList();
        Map<String, RegistrationStatus> registrationStatusByEventId =
                getRegistrationStatusMap(currentUserId, eventIds);

        responses.forEach(response ->
                response.setRegistrationStatus(registrationStatusByEventId.get(response.getId())));
        return responses;
    }

    private Map<String, RegistrationStatus> getRegistrationStatusMap(String userId, Collection<String> eventIds) {
        if (userId == null || eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        Map<String, RegistrationStatus> registrationStatusByEventId = new HashMap<>();
        for (EventRegistrationEntity registration : eventRegistrationRepository.findByUserIdAndEventIdIn(userId, eventIds)) {
            if (registration.getEvent() == null || registration.getEvent().getId() == null) {
                continue;
            }
            registrationStatusByEventId.putIfAbsent(
                    registration.getEvent().getId(),
                    registration.getRegistrationStatus());
        }

        return registrationStatusByEventId;
    }

    private EventResponseDTO enrichEventResponseWithRegistrationStatus(
            EventResponseDTO response,
            String eventId,
            String userId) {
        if (response == null || eventId == null || userId == null) {
            return response;
        }

        RegistrationStatus registrationStatus = eventRegistrationRepository
                .findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId)
                .map(EventRegistrationEntity::getRegistrationStatus)
                .orElse(null);
        response.setRegistrationStatus(registrationStatus);
        return response;
    }

    private String tryGetCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank() || "anonymousUser".equals(principalName)) {
            return null;
        }

        return principalName;
    }
}
