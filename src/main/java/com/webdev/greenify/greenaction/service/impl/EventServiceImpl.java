package com.webdev.greenify.greenaction.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.common.util.TimeUtils;
import com.webdev.greenify.file.entity.EventImageEntity;
import com.webdev.greenify.file.enumeration.EventImageType;
import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.request.EventStatusRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.greenaction.mapper.EventMapper;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.greenaction.service.EventService;
import com.webdev.greenify.greenaction.specification.EventSpecification;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final ImageMapper imageMapper;

    @Override
    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        String currentUserId = getCurrentUserId();
        UserEntity organizer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        EventEntity event = eventMapper.toEntity(request);
        event.setOrganizer(organizer);

        if (event.getStatus() == null) {
            event.setStatus(GreenEventStatus.DRAFT);
        }

        prepareEventRelationships(event, request);

        event = eventRepository.save(event);
        log.info("Event created with ID: {} by user: {}", event.getId(), currentUserId);

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponseDTO> getEventsWithFilter(
            GreenEventStatus status,
            GreenEventType eventType,
            String title,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<EventEntity> spec = EventSpecification.buildSpecification(status, eventType, title, from, to);
        
        Page<EventEntity> eventPage = eventRepository.findAll(spec, pageable);

        List<EventResponseDTO> content = eventPage.getContent().stream()
                .map(eventMapper::toResponse)
                .toList();

        return PagedResponse.of(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDTO getEventDetail(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        if (event.isDeleted()) {
            throw new ResourceNotFoundException("Event not found");
        }
        
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public EventResponseDTO updateEvent(String id, EventRequestDTO request) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.isDeleted()) {
            throw new ResourceNotFoundException("Event not found");
        }

        // Check if status allows update
        EnumSet<GreenEventStatus> allowableStatuses = EnumSet.of(
                GreenEventStatus.DRAFT, 
                GreenEventStatus.APPROVAL_WAITING, 
                GreenEventStatus.REJECTED);
        
        if (!allowableStatuses.contains(event.getStatus())) {
            throw new AppException("Cannot update event in current status: " + event.getStatus(), HttpStatus.BAD_REQUEST);
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());

        // Update basic fields
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setMaxParticipants(request.getMaxParticipants());
        event.setMinParticipants(request.getMinParticipants());
        event.setCancelDeadlineHoursBefore(request.getCancelDeadlineHoursBefore());
        event.setSignUpDeadlineHoursBefore(request.getSignUpDeadlineHoursBefore());
        event.setReminderHoursBefore(request.getReminderHoursBefore());
        event.setThankYouHoursAfter(request.getThankYouHoursAfter());
        event.setRewardPoints(request.getRewardPoints());
        
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        // Handle image updates (for simplicity here, we clear and re-add if needed or just handle thumbnail)
        // In a real app, you'd probably handle this more granularly
        if (request.getThumbnail() != null) {
            // Remove old thumbnail if exists
            event.getImages().removeIf(img -> img.getImageType() == EventImageType.THUMBNAIL);
            EventImageEntity eventThumbnail = imageMapper.toEventImageEntity(request.getThumbnail());
            eventThumbnail.setEvent(event);
            eventThumbnail.setImageType(EventImageType.THUMBNAIL);
            event.getImages().add(eventThumbnail);
        }

        event = eventRepository.save(event);
        log.info("Event updated with ID: {}", event.getId());

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public void deleteEvent(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        event.setDeleted(true);
        eventRepository.save(event);
        log.info("Event soft-deleted with ID: {}", id);
    }

    @Override
    @Transactional
    public void approveEvent(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        if (event.getStatus() != GreenEventStatus.APPROVAL_WAITING) {
            throw new AppException("Event is not waiting for approval", HttpStatus.BAD_REQUEST);
        }

        event.setStatus(GreenEventStatus.PUBLISHED);
        event.setRejectReason(null);
        eventRepository.save(event);
        log.info("Event approved with ID: {}", id);
    }

    @Override
    @Transactional
    public void rejectEvent(String id, EventStatusRequestDTO request) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        if (event.getStatus() != GreenEventStatus.APPROVAL_WAITING) {
            throw new AppException("Event is not waiting for approval", HttpStatus.BAD_REQUEST);
        }

        event.setStatus(GreenEventStatus.REJECTED);
        event.setRejectReason(request.getReason());
        event.setRejectedCount(event.getRejectedCount() + 1);
        eventRepository.save(event);
        log.info("Event rejected with ID: {} Reason: {}", id, request.getReason());
    }

    private void prepareEventRelationships(EventEntity event, EventRequestDTO request) {
        if (event.getAddress() != null) {
            event.getAddress().setEvent(event);
        }

        if (event.getImages() != null && !event.getImages().isEmpty()) {
            event.getImages().forEach(image -> {
                image.setEvent(event);
                image.setImageType(EventImageType.DETAIL);
            });
        }

        if (request.getThumbnail() != null) {
            EventImageEntity eventThumbnail = imageMapper.toEventImageEntity(request.getThumbnail());
            eventThumbnail.setEvent(event);
            eventThumbnail.setImageType(EventImageType.THUMBNAIL);
            event.getImages().add(eventThumbnail);
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!TimeUtils.isValidTime(start, end)) {
            throw new AppException("Start time must be before end time", HttpStatus.BAD_REQUEST);
        }
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
