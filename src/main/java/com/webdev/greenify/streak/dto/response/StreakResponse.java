package com.webdev.greenify.streak.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webdev.greenify.streak.enumeration.StreakStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreakResponse {

    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate lastValidDate;
    private StreakStatus status;
    private Integer restoreUsedThisMonth;
    private Boolean restoreAvailable;
}
