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
public class AnalystDashboardDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private AnalystMetricDTO totalMetrics;
    private List<MonthlyMetricDTO> monthlyBreakdown;
}
