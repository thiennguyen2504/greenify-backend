package com.webdev.greenify.leaderboard.service;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.leaderboard.dto.request.CreatePrizeConfigRequest;
import com.webdev.greenify.leaderboard.dto.response.LeaderboardPrizeResponse;
import com.webdev.greenify.leaderboard.dto.response.LeaderboardResponse;
import com.webdev.greenify.leaderboard.dto.response.PrizeConfigResponse;
import com.webdev.greenify.leaderboard.enumeration.LeaderboardScope;
import com.webdev.greenify.leaderboard.enumeration.PrizeConfigStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface LeaderboardService {

    PrizeConfigResponse createPrizeConfig(CreatePrizeConfigRequest request);

    PagedResponse<PrizeConfigResponse> getPrizeConfigs(
            LocalDate weekStartDate,
            PrizeConfigStatus status,
            int page,
            int size);

    PrizeConfigResponse getPrizeConfigById(String id);

    void cancelPrizeConfig(String id);

    void finalizeDueWeeks();

    void finalizeWeek(String prizeConfigId);

    void updateScore(String userId, BigDecimal weeklyPoints, LocalDateTime lastPointEarnedAt);

    LeaderboardResponse getLeaderboard(LocalDate weekStartDate, LeaderboardScope scope, String province);

    LeaderboardPrizeResponse getLeaderboardPrize(LocalDate weekStartDate);
}
