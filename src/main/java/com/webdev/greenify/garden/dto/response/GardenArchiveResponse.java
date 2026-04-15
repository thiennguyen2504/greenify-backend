package com.webdev.greenify.garden.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.garden.enumeration.GardenRewardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GardenArchiveResponse {

    private String seedName;
    private String displayImageUrl;
    private Integer daysTaken;
    private GardenRewardStatus rewardStatus;
    private String voucherCode;
    private LocalDateTime archivedAt;
}
