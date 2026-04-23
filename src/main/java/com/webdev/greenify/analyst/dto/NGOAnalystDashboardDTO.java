package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NGOAnalystDashboardDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private NGOAnalystMetricDTO totalMetrics;
    private List<NGOMonthlyMetricDTO> monthlyBreakdown;
}
