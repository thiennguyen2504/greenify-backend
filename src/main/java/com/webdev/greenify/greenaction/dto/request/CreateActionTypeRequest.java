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

    @NotBlank(message = "Tên nhóm là bắt buộc")
    @Size(max = 100, message = "Tên nhóm không được vượt quá 100 ký tự")
    private String groupName;

    @NotBlank(message = "Tên hành động là bắt buộc")
    @Size(max = 200, message = "Tên hành động không được vượt quá 200 ký tự")
    private String actionName;

    @NotNull(message = "Điểm gợi ý là bắt buộc")
    @DecimalMin(value = "0.01", message = "Điểm gợi ý phải lớn hơn 0")
    private BigDecimal suggestedPoints;

    @NotNull(message = "Thuộc tính yêu cầu vị trí là bắt buộc")
    private Boolean locationRequired;

    @Builder.Default
    private Boolean isActive = true;
}
