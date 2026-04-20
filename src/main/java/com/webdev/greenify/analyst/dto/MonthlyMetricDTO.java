package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyMetricDTO {
    private String month;
    private long verifiedPosts;
    private long eventAttendance;
    private long trashResolved;
}
