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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final ImageMapper imageMapper;

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
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

        return eventMapper.toResponse(event);
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
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        if (event.isDeleted()) {
            throw new ResourceNotFoundException("Event not found");
        }

        if (isPublicUser() && SENSITIVE_STATUSES.contains(event.getStatus())) {
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

        EnumSet<GreenEventStatus> allowableStatuses = EnumSet.of(
                GreenEventStatus.DRAFT, 
                GreenEventStatus.APPROVAL_WAITING, 
                GreenEventStatus.REJECTED);
        
        if (!allowableStatuses.contains(event.getStatus())) {
            throw new AppException("Cannot update event in current status: " + event.getStatus(), HttpStatus.BAD_REQUEST);
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());
        eventMapper.updateEvent(request, event);
        prepareEventRelationships(event, request);
        event.setStatus(GreenEventStatus.APPROVAL_WAITING);

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
        Integer newRejectedCount = event.getRejectedCount() != null ? event.getRejectedCount() + 1 : 1;
        event.setRejectedCount(newRejectedCount);
        eventRepository.save(event);
        log.info("Event rejected with ID: {} Reason: {}", id, request.getReason());
    }

    @Override
    @Transactional
    public void submitEvent(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        if (event.getStatus() != GreenEventStatus.DRAFT && event.getStatus() != GreenEventStatus.REJECTED) {
            throw new AppException("Only DRAFT or REJECTED events can be submitted", HttpStatus.BAD_REQUEST);
        }

        event.setStatus(GreenEventStatus.APPROVAL_WAITING);
        eventRepository.save(event);
        log.info("Event submitted for approval: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponseDTO> getMyEvents(int page, int size) {
        String currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<EventEntity> spec = EventSpecification.buildSpecification(null, null, null, null, null, currentUserId);
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
            throw new AppException("Start time must be before end time", HttpStatus.BAD_REQUEST);
        }
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
