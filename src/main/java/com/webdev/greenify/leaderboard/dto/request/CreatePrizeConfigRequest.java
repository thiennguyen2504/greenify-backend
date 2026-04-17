package com.webdev.greenify.leaderboard.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatePrizeConfigRequest {

    @NotBlank(message = "weekStartDate is required")
    private String weekStartDate;

    @NotBlank(message = "lockAt is required")
    private String lockAt;

    @NotBlank(message = "nationalVoucherTemplateId is required")
    private String nationalVoucherTemplateId;

    @NotBlank(message = "provincialVoucherTemplateId is required")
    private String provincialVoucherTemplateId;
}
