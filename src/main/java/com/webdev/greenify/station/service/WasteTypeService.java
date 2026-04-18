package com.webdev.greenify.station.service;

import com.webdev.greenify.station.dto.WasteTypeResponseDTO;

import java.util.List;

public interface WasteTypeService {
    List<WasteTypeResponseDTO> getAllWasteTypes();
}