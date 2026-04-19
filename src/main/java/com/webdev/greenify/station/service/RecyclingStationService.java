package com.webdev.greenify.station.service;

import com.webdev.greenify.station.dto.RecyclingStationRequestDTO;
import com.webdev.greenify.station.dto.RecyclingStationResponseDTO;
import com.webdev.greenify.station.dto.UpdateStationStatusRequestDTO;

import java.util.List;

public interface RecyclingStationService {
    RecyclingStationResponseDTO createStation(RecyclingStationRequestDTO request);

    List<RecyclingStationResponseDTO> getAllStations(String wasteTypeId);

    RecyclingStationResponseDTO getStationById(String id);

    RecyclingStationResponseDTO updateStation(String id, RecyclingStationRequestDTO request);

    RecyclingStationResponseDTO updateStationStatus(String id, UpdateStationStatusRequestDTO request);

    void deleteStation(String id);
}
