package com.webdev.greenify.analyst.service;

import com.webdev.greenify.analyst.dto.AnalystDashboardDTO;
import com.webdev.greenify.analyst.dto.Co2eDashboardDTO;
import com.webdev.greenify.analyst.dto.LandingPageMetricsDTO;
import com.webdev.greenify.analyst.dto.NGOAnalystDashboardDTO;

import java.time.LocalDate;

public interface AnalystService {
    AnalystDashboardDTO getAdminDashboardMetrics(LocalDate startDate, LocalDate endDate);

    Co2eDashboardDTO getCo2eDashboardMetrics(LocalDate startDate, LocalDate endDate);
    
    NGOAnalystDashboardDTO getNGODashboardMetrics(String ngoId, LocalDate startDate, LocalDate endDate);

    LandingPageMetricsDTO getLandingPageMetrics();
}
