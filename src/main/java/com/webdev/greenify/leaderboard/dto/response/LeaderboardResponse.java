package com.webdev.greenify.leaderboard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.leaderboard.enumeration.LeaderboardScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardResponse {

    private LocalDate weekStartDate;
    private LeaderboardScope scope;
    private String province;
    private List<LeaderboardEntryResponse> entries;
}
