package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.request.CreateActionTypeRequest;
import com.webdev.greenify.greenaction.dto.request.UpdateActionTypeRequest;
import com.webdev.greenify.greenaction.dto.response.GreenActionTypeResponse;
import com.webdev.greenify.greenaction.service.GreenActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GreenActionTypeController {

    private final GreenActionService greenActionService;

    @GetMapping("/green-action/action-types")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<List<GreenActionTypeResponse>> getAllActionTypes() {
        return ResponseEntity.ok(greenActionService.getAllActionTypes());
    }

    @PostMapping("/admin/green-action/action-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GreenActionTypeResponse> createActionType(
            @Valid @RequestBody CreateActionTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(greenActionService.createActionType(request));
    }

    @PatchMapping("/admin/green-action/action-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GreenActionTypeResponse> updateActionType(
            @PathVariable String id,
            @Valid @RequestBody UpdateActionTypeRequest request) {
        return ResponseEntity.ok(greenActionService.updateActionType(id, request));
    }
}