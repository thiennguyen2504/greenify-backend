package com.webdev.greenify.station.dto;

import com.webdev.greenify.station.enumeration.StationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStationStatusRequestDTO {
    @NotNull(message = "Trạng thái không được để trống")
    private StationStatus status;
}
