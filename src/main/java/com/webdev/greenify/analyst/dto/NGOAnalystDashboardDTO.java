package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NGOAnalystDashboardDTO {
    private NGOAnalystMetricDTO totalMetrics;
    private List<NGOMonthlyMetricDTO> monthlyBreakdown;
}
