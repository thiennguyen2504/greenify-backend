package com.webdev.greenify.greenaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
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
public class UpdateActionTypeRequest {

    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String groupName;

    @Size(max = 200, message = "Action name must not exceed 200 characters")
    private String actionName;

    @DecimalMin(value = "0.01", message = "Suggested points must be greater than 0")
    private BigDecimal suggestedPoints;

    private Boolean locationRequired;

    private Boolean isActive;
}
