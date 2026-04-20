package com.webdev.greenify.analyst.controller;

import com.webdev.greenify.analyst.dto.AnalystDashboardDTO;
import com.webdev.greenify.analyst.dto.NGOAnalystDashboardDTO;
import com.webdev.greenify.analyst.service.AnalystService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analyst")
@RequiredArgsConstructor
public class AnalystController {

    private final AnalystService analystService;

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalystDashboardDTO> getAdminDashboard(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return ResponseEntity.ok(analystService.getAdminDashboardMetrics(startDate, endDate));
    }

    @GetMapping("/ngo/dashboard")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<NGOAnalystDashboardDTO> getNGODashboard(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        String ngoId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(analystService.getNGODashboardMetrics(ngoId, startDate, endDate));
    }
}
