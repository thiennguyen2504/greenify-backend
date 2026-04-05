package com.webdev.greenify.station.service;

import com.webdev.greenify.station.dto.RecyclingStationRequestDTO;
import com.webdev.greenify.station.dto.RecyclingStationResponseDTO;
import com.webdev.greenify.station.dto.UpdateStationStatusRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecyclingStationService {
    RecyclingStationResponseDTO createStation(RecyclingStationRequestDTO request);

    Page<RecyclingStationResponseDTO> getAllStations(Pageable pageable);

    RecyclingStationResponseDTO getStationById(String id);

    RecyclingStationResponseDTO updateStation(String id, RecyclingStationRequestDTO request);

    RecyclingStationResponseDTO updateStationStatus(String id, UpdateStationStatusRequestDTO request);

    void deleteStation(String id);
}
