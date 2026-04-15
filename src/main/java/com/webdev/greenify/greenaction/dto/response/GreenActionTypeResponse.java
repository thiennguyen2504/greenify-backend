package com.webdev.greenify.greenaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreenActionTypeResponse {

    private String id;
    private String groupName;
    private String actionName;
    private BigDecimal suggestedPoints;
    private Boolean locationRequired;
    private Boolean isActive;
}