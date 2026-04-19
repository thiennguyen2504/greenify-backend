package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NGOAnalystMetricDTO {
    private long totalEvents;
    private long totalParticipants;
    private double averageAttendanceRate;
}
