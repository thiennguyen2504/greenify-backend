package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageMetricsDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private long totalPosts;
    private long totalUsers;
    private long totalStations;
    private long totalEvents;
}
