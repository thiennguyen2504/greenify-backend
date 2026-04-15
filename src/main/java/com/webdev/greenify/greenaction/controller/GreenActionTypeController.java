package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.response.GreenActionTypeResponse;
import com.webdev.greenify.greenaction.service.GreenActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/green-action/action-types")
@RequiredArgsConstructor
public class GreenActionTypeController {

    private final GreenActionService greenActionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<List<GreenActionTypeResponse>> getAllActionTypes() {
        return ResponseEntity.ok(greenActionService.getAllActionTypes());
    }
}