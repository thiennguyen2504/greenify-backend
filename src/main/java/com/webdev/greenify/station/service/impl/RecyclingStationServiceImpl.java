package com.webdev.greenify.station.service.impl;

import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.station.dto.RecyclingStationRequestDTO;
import com.webdev.greenify.station.dto.RecyclingStationResponseDTO;
import com.webdev.greenify.station.dto.UpdateStationStatusRequestDTO;
import com.webdev.greenify.station.entity.RecyclingStationEntity;
import com.webdev.greenify.station.entity.WasteTypeEntity;
import com.webdev.greenify.station.enumeration.StationStatus;
import com.webdev.greenify.station.mapper.RecyclingStationMapper;
import com.webdev.greenify.station.repository.RecyclingStationRepository;
import com.webdev.greenify.station.repository.WasteTypeRepository;
import com.webdev.greenify.station.service.RecyclingStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecyclingStationServiceImpl implements RecyclingStationService {

    private final RecyclingStationRepository recyclingStationRepository;
    private final WasteTypeRepository wasteTypeRepository;
    private final RecyclingStationMapper recyclingStationMapper;

    @Override
    @Transactional
    public RecyclingStationResponseDTO createStation(RecyclingStationRequestDTO request) {
        RecyclingStationEntity stationEntity = recyclingStationMapper.toEntity(request);

        if (request.getWasteTypeIds() != null && !request.getWasteTypeIds().isEmpty()) {
            List<WasteTypeEntity> wasteTypes = wasteTypeRepository.findAllById(request.getWasteTypeIds());
            if (wasteTypes.size() != request.getWasteTypeIds().size()) {
                throw new ResourceNotFoundException("Some waste types were not found.");
            }
            stationEntity.setWasteTypes(wasteTypes);
        }
        stationEntity.setStatus(StationStatus.ACTIVE);
        stationEntity.getOpenTimes().forEach(openTime -> openTime.setRecyclingStation(stationEntity));
        RecyclingStationEntity savedStation = recyclingStationRepository.save(stationEntity);
        return recyclingStationMapper.toRecyclingStationResponseDTO(savedStation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecyclingStationResponseDTO> getAllStations(Pageable pageable) {
        return recyclingStationRepository.findByIsDeletedFalse(pageable)
                .map(recyclingStationMapper::toRecyclingStationResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public RecyclingStationResponseDTO getStationById(String id) {
        return recyclingStationRepository.findByIdAndIsDeletedFalse(id)
                .map(recyclingStationMapper::toRecyclingStationResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Recycling station not found with id: " + id));
    }

    @Override
    @Transactional
    public RecyclingStationResponseDTO updateStation(String id, RecyclingStationRequestDTO request) {
        RecyclingStationEntity stationEntity = recyclingStationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recycling station not found with id: " + id));

        recyclingStationMapper.updateEntity(stationEntity, request);

        if (request.getWasteTypeIds() != null) {
            List<WasteTypeEntity> newWasteTypes = wasteTypeRepository.findAllById(request.getWasteTypeIds());
            if (newWasteTypes.size() != request.getWasteTypeIds().size()) {
                throw new ResourceNotFoundException("Some waste types were not found.");
            }
            List<WasteTypeEntity> currentWasteTypes = stationEntity.getWasteTypes();
            currentWasteTypes.removeIf(wt -> !request.getWasteTypeIds().contains(wt.getId()));
            for (WasteTypeEntity newWt : newWasteTypes) {
                boolean exists = currentWasteTypes.stream()
                        .anyMatch(currentWt -> currentWt.getId().equals(newWt.getId()));
                if (!exists) {
                    currentWasteTypes.add(newWt);
                }
            }
        }
        stationEntity.getOpenTimes().forEach(openTime -> openTime.setRecyclingStation(stationEntity));
        RecyclingStationEntity savedStation = recyclingStationRepository.save(stationEntity);
        return recyclingStationMapper.toRecyclingStationResponseDTO(savedStation);
    }

    @Override
    @Transactional
    public RecyclingStationResponseDTO updateStationStatus(String id, UpdateStationStatusRequestDTO request) {
        RecyclingStationEntity stationEntity = recyclingStationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recycling station not found with id: " + id));

        stationEntity.setStatus(request.getStatus());
        RecyclingStationEntity savedStation = recyclingStationRepository.save(stationEntity);
        return recyclingStationMapper.toRecyclingStationResponseDTO(savedStation);
    }

    @Override
    @Transactional
    public void deleteStation(String id) {
        RecyclingStationEntity stationEntity = recyclingStationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recycling station not found with id: " + id));
        stationEntity.setDeleted(true);
        recyclingStationRepository.save(stationEntity);
    }
}
