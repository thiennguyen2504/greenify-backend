package com.webdev.greenify.station.service;

import com.webdev.greenify.station.dto.ProvinceResponseDTO;
import com.webdev.greenify.station.dto.WardResponseDTO;

import java.util.List;

public interface AdministrativeDivisionService {
    List<ProvinceResponseDTO> getProvinces();

    List<WardResponseDTO> getWardsByProvinceCode(int provinceCode);
}