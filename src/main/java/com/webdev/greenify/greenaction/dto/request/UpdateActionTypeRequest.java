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

    @Size(max = 100, message = "Tên nhóm không được vượt quá 100 ký tự")
    private String groupName;

    @Size(max = 200, message = "Tên hành động không được vượt quá 200 ký tự")
    private String actionName;

    @DecimalMin(value = "0.01", message = "Điểm gợi ý phải lớn hơn 0")
    private BigDecimal suggestedPoints;

    private Boolean locationRequired;

    private Boolean isActive;
}
