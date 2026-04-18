package com.webdev.greenify.station.service.impl;

import com.webdev.greenify.station.dto.WasteTypeResponseDTO;
import com.webdev.greenify.station.mapper.WasteTypeMapper;
import com.webdev.greenify.station.repository.WasteTypeRepository;
import com.webdev.greenify.station.service.WasteTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WasteTypeServiceImpl implements WasteTypeService {

    private final WasteTypeRepository wasteTypeRepository;
    private final WasteTypeMapper wasteTypeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WasteTypeResponseDTO> getAllWasteTypes() {
        return wasteTypeRepository.findAllByOrderByNameAsc().stream()
                .map(wasteTypeMapper::toWasteTypeResponseDTO)
                .toList();
    }
}