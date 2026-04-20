package com.webdev.greenify.greenaction.dto.response;

import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import com.webdev.greenify.station.dto.AddressResponseDTO;
import com.webdev.greenify.user.dto.NGOPreviewDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private String id;
    private String title;
    private String description;
    private GreenEventType eventType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long maxParticipants;
    private Long minParticipants;
    private Long cancelDeadlineHoursBefore;
    private Long signUpDeadlineHoursBefore;
    private Long reminderHoursBefore;
    private Long thankYouHoursAfter;
    private Double rewardPoints;
    private GreenEventStatus status;
    private RegistrationStatus registrationStatus;
    private String rejectReason;
    private Integer rejectedCount;
    private AddressResponseDTO address;
    private EventImageResponseDTO thumbnail;
    private List<EventImageResponseDTO> images;
    private String participationConditions;
    private Long participantCount;
    private NGOPreviewDTO organizer;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
