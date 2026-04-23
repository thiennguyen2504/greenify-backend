package com.webdev.greenify.analyst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalystMetricDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private long newUsers;
    private long verifiedPosts;
    private BigDecimal pointsIssued;
    private long vouchersRedeemed;
    private long eventAttendance;
    private long trashResolved;
}
