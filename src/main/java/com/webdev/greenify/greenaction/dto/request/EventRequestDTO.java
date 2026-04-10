package com.webdev.greenify.greenaction.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.station.dto.AddressRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Event type is required")
    private GreenEventType eventType;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Min(value = 1, message = "Max participants must be at least 1")
    private Long maxParticipants;

    @Min(value = 1, message = "Max participants must be at least 1")
    private Long minParticipants;

    @Positive(message = "Cancel deadline hours must be positive")
    private Long cancelDeadlineHoursBefore;

    @Positive(message = "Sign up deadline hours must be positive")
    private Long signUpDeadlineHoursBefore;

    @Positive(message = "Reminder hours before must be positive")
    private Long reminderHoursBefore;

    @Positive(message = "Thank you hours after must be positive")
    private Long thankYouHoursAfter;

    @NotNull(message = "Reward points are required")
    @Positive(message = "Reward points must be positive")
    private Double rewardPoints;

    private GreenEventStatus status;

    @Valid
    @NotNull(message = "Address is required")
    private ImageRequestDTO thumbnail;

    @Valid
    private List<ImageRequestDTO> images;

    @NotNull(message = "Address is required")
    @Valid
    private AddressRequestDTO address;
}
