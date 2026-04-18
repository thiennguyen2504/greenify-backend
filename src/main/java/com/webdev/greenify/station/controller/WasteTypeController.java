package com.webdev.greenify.station.controller;

import com.webdev.greenify.station.dto.WasteTypeResponseDTO;
import com.webdev.greenify.station.service.WasteTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/waste-types")
@RequiredArgsConstructor
public class WasteTypeController {

    private final WasteTypeService wasteTypeService;

    @GetMapping
    public ResponseEntity<List<WasteTypeResponseDTO>> getAllWasteTypes() {
        return ResponseEntity.ok(wasteTypeService.getAllWasteTypes());
    }
}