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

    @NotBlank(message = "weekStartDate là bắt buộc")
    private String weekStartDate;

    @NotBlank(message = "lockAt là bắt buộc")
    private String lockAt;

    @NotBlank(message = "nationalVoucherTemplateId là bắt buộc")
    private String nationalVoucherTemplateId;

    @NotBlank(message = "provincialVoucherTemplateId là bắt buộc")
    private String provincialVoucherTemplateId;
}
