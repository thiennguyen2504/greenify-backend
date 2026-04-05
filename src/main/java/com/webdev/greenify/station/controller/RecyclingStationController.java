package com.webdev.greenify.station.controller;

import com.webdev.greenify.station.dto.RecyclingStationRequestDTO;
import com.webdev.greenify.station.dto.RecyclingStationResponseDTO;
import com.webdev.greenify.station.service.RecyclingStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recycling-stations")
@RequiredArgsConstructor
public class RecyclingStationController {

    private final RecyclingStationService recyclingStationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecyclingStationResponseDTO> createStation(@RequestBody @Valid RecyclingStationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recyclingStationService.createStation(request));
    }

    @GetMapping
    public ResponseEntity<List<RecyclingStationResponseDTO>> getAllStations() {
        return ResponseEntity.ok(recyclingStationService.getAllStations());
    }
}
