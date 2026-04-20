package com.webdev.greenify.greenaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppealRequest {

    @NotBlank(message = "Lý do khiếu nại là bắt buộc")
    private String appealReason;

    private List<String> evidenceUrls;
}
