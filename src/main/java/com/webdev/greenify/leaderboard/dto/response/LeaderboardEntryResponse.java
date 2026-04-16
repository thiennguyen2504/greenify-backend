package com.webdev.greenify.leaderboard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardEntryResponse {

    private Integer rank;
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String province;
    private BigDecimal weeklyPoints;
}
