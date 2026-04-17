package com.webdev.greenify.greenaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActionTypeRequest {

    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String groupName;

    @NotBlank(message = "Action name is required")
    @Size(max = 200, message = "Action name must not exceed 200 characters")
    private String actionName;

    @NotNull(message = "Suggested points is required")
    @DecimalMin(value = "0.01", message = "Suggested points must be greater than 0")
    private BigDecimal suggestedPoints;

    @NotNull(message = "Location required is required")
    private Boolean locationRequired;

    @Builder.Default
    private Boolean isActive = true;
}
