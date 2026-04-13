package com.webdev.greenify.garden.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectSeedRequest {

    @NotBlank(message = "Seed ID is required")
    private String seedId;
}
