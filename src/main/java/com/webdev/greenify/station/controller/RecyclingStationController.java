package com.webdev.greenify.station.controller;

import com.webdev.greenify.station.dto.RecyclingStationRequestDTO;
import com.webdev.greenify.station.dto.RecyclingStationResponseDTO;
import com.webdev.greenify.station.dto.UpdateStationStatusRequestDTO;
import com.webdev.greenify.station.service.RecyclingStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'NGO', 'ADMIN')")
    public ResponseEntity<List<RecyclingStationResponseDTO>> getAllStations(
            @RequestParam(name = "wasteTypeID", required = false) String wasteTypeId) {
        return ResponseEntity.ok(recyclingStationService.getAllStations(wasteTypeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'NGO', 'ADMIN')")
    public ResponseEntity<RecyclingStationResponseDTO> getStationById(@PathVariable String id) {
        return ResponseEntity.ok(recyclingStationService.getStationById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecyclingStationResponseDTO> updateStation(@PathVariable String id, @RequestBody @Valid RecyclingStationRequestDTO request) {
        return ResponseEntity.ok(recyclingStationService.updateStation(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecyclingStationResponseDTO> updateStationStatus(@PathVariable String id, @RequestBody @Valid UpdateStationStatusRequestDTO request) {
        return ResponseEntity.ok(recyclingStationService.updateStationStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStation(@PathVariable String id) {
        recyclingStationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}
