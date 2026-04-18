package com.webdev.greenify.greenaction.dto.request;

import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventPredictionRequestDTO {
    @NotBlank
    private String province;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotNull
    @Min(0)
    private Long minParticipants;

    @NotNull
    @Min(0)
    private Long expectedParticipants;

    @NotNull
    private GreenEventType eventType;
}
