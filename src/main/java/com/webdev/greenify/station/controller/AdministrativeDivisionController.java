package com.webdev.greenify.station.controller;

import com.webdev.greenify.station.dto.ProvinceResponseDTO;
import com.webdev.greenify.station.dto.WardResponseDTO;
import com.webdev.greenify.station.service.AdministrativeDivisionService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/divisions")
@RequiredArgsConstructor
@Validated
public class AdministrativeDivisionController {

    private final AdministrativeDivisionService administrativeDivisionService;

    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceResponseDTO>> getProvinces() {
        return ResponseEntity.ok(administrativeDivisionService.getProvinces());
    }

    @GetMapping("/provinces/{provinceCode}/wards")
    public ResponseEntity<List<WardResponseDTO>> getWardsByProvinceCode(
            @PathVariable @Min(value = 1, message = "provinceCode phải lớn hơn 0") int provinceCode) {
        return ResponseEntity.ok(administrativeDivisionService.getWardsByProvinceCode(provinceCode));
    }
}