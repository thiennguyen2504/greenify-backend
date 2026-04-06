package com.webdev.greenify.station.dto;

import com.webdev.greenify.station.enumeration.StationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecyclingStationResponseDTO {
    private String id;
    private String name;
    private String description;
    private String phoneNumber;
    private String email;
    private StationStatus status;
    private AddressResponseDTO address;
    private List<WasteTypeResponseDTO> wasteTypes;
    private List<OpenTimeResponseDTO> openTimes;
}
