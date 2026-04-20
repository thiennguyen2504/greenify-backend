package com.webdev.greenify.greenaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipationSummaryResponseDTO {

    private long registeredCount;
    private long waitlistedCount;
    private long cancelledCount;
    private long attendedCount;
}
