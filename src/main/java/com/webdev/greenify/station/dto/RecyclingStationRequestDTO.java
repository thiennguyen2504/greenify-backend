package com.webdev.greenify.station.dto;

import jakarta.validation.constraints.NotBlank;
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
public class RecyclingStationRequestDTO {
    @NotBlank(message = "Recycling station name cannot be blank")
    private String name;
    private String description;
    private String phoneNumber;
    private String email;
    private AddressRequestDTO address;
    private List<String> wasteTypeIds;
    private List<OpenTimeRequestDTO> openTimes;
}
