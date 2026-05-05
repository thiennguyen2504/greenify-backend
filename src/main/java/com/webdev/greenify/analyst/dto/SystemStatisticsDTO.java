package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatisticsDTO {
    private long totalGreenActionPosts;
    private long totalUsers;
    private long totalRecyclingStations;
    private long totalEvents;
}
