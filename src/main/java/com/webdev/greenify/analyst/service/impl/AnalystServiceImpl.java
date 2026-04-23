package com.webdev.greenify.analyst.service.impl;

import com.webdev.greenify.analyst.dto.AnalystDashboardDTO;
import com.webdev.greenify.analyst.dto.AnalystMetricDTO;
import com.webdev.greenify.analyst.dto.MonthlyMetricDTO;
import com.webdev.greenify.analyst.dto.NGOAnalystDashboardDTO;
import com.webdev.greenify.analyst.dto.NGOAnalystMetricDTO;
import com.webdev.greenify.analyst.dto.NGOMonthlyMetricDTO;
import com.webdev.greenify.analyst.service.AnalystService;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import com.webdev.greenify.greenaction.repository.EventRegistrationRepository;
import com.webdev.greenify.greenaction.repository.EventRepository;
import com.webdev.greenify.greenaction.repository.GreenActionPostRepository;
import com.webdev.greenify.greenaction.repository.PointTransactionRepository;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;
import com.webdev.greenify.trashspot.repository.TrashSpotRepository;
import com.webdev.greenify.trashspot.repository.TrashSpotResolveRequestRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.voucher.enumeration.UserVoucherStatus;
import com.webdev.greenify.voucher.repository.UserVoucherRepository;
import com.webdev.greenify.voucher.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalystServiceImpl implements AnalystService {

    private final UserRepository userRepository;
    private final GreenActionPostRepository postRepository;
    private final PointTransactionRepository pointRepository;
    private final UserVoucherRepository voucherRepository;
    private final EventRegistrationRepository registrationRepository;
    private final TrashSpotResolveRequestRepository resolveRepository;
    private final EventRepository eventRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final TrashSpotRepository trashSpotRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "adminDashboard", key = "{#startDate, #endDate}")
    public AnalystDashboardDTO getAdminDashboardMetrics(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        AnalystMetricDTO totalMetrics = calculateAdminMetrics(startDateTime, endDateTime);
        List<MonthlyMetricDTO> monthlyBreakdown = calculateAdminMonthlyBreakdown(startDate, endDate);

        return AnalystDashboardDTO.builder()
                .totalMetrics(totalMetrics)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "ngoDashboard", key = "{#ngoId, #startDate, #endDate}")
    public NGOAnalystDashboardDTO getNGODashboardMetrics(String ngoId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        NGOAnalystMetricDTO totalMetrics = calculateNGOMetrics(ngoId, startDateTime, endDateTime);
        List<NGOMonthlyMetricDTO> monthlyBreakdown = calculateNGOMonthlyBreakdown(ngoId, startDate, endDate);

        return NGOAnalystDashboardDTO.builder()
                .totalMetrics(totalMetrics)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    private AnalystMetricDTO calculateAdminMetrics(LocalDateTime start, LocalDateTime end) {
        return AnalystMetricDTO.builder()
                .newUsers(userRepository.countByCreatedAtBetween(start, end))
                .verifiedPosts(postRepository.countByCreatedAtBetweenAndStatus(start, end, PostStatus.VERIFIED))
                .pointsIssued(pointRepository.sumPointsByCreatedAtBetween(start, end))
                .vouchersRedeemed(voucherTemplateRepository.countByCreatedAtBetweenAndIsDeletedFalse(start, end))
                .eventAttendance(eventRepository.sumParticipantCountByStatusAndEndTimeBetween(GreenEventStatus.COMPLETED, start, end))
                .trashResolved(trashSpotRepository.countByStatusAndLastModifiedAtBetweenAndIsDeletedFalse(TrashSpotStatus.VERIFIED, start, end))
                .build();
    }

    private List<MonthlyMetricDTO> calculateAdminMonthlyBreakdown(LocalDate startDate, LocalDate endDate) {
        List<MonthlyMetricDTO> breakdown = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            LocalDateTime monthStart = getMonthStart(month, startDate);
            LocalDateTime monthEnd = getMonthEnd(month, endDate);

            AnalystMetricDTO metrics = calculateAdminMetrics(monthStart, monthEnd);
            breakdown.add(MonthlyMetricDTO.builder()
                    .month(month.toString())
                    .verifiedPosts(metrics.getVerifiedPosts())
                    .eventAttendance(metrics.getEventAttendance())
                    .trashResolved(metrics.getTrashResolved())
                    .build());
        }
        return breakdown;
    }

    private NGOAnalystMetricDTO calculateNGOMetrics(String ngoId, LocalDateTime start, LocalDateTime end) {
        long totalEvents = eventRepository.countByOrganizerIdAndCreatedAtBetween(ngoId, start, end);
        long totalParticipants = eventRepository.sumParticipantCountByOrganizerIdAndCreatedAtBetween(ngoId, start, end);
        long attendedCount = registrationRepository.countByOrganizerIdAndStatusAndCreatedAtBetween(ngoId, RegistrationStatus.ATTENDED, start, end);
        
        double attendanceRate = totalParticipants > 0 ? (double) attendedCount / totalParticipants : 0.0;
        
        return NGOAnalystMetricDTO.builder()
                .totalEvents(totalEvents)
                .totalParticipants(totalParticipants)
                .averageAttendanceRate(attendanceRate)
                .build();
    }

    private List<NGOMonthlyMetricDTO> calculateNGOMonthlyBreakdown(String ngoId, LocalDate startDate, LocalDate endDate) {
        List<NGOMonthlyMetricDTO> breakdown = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            LocalDateTime monthStart = getMonthStart(month, startDate);
            LocalDateTime monthEnd = getMonthEnd(month, endDate);

            NGOAnalystMetricDTO metrics = calculateNGOMetrics(ngoId, monthStart, monthEnd);
            breakdown.add(NGOMonthlyMetricDTO.builder()
                    .month(month.toString())
                    .totalEvents(metrics.getTotalEvents())
                    .totalParticipants(metrics.getTotalParticipants())
                    .build());
        }
        return breakdown;
    }

    private LocalDateTime getMonthStart(YearMonth month, LocalDate requestedStart) {
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        return monthStart.isBefore(requestedStart.atStartOfDay()) ? requestedStart.atStartOfDay() : monthStart;
    }

    private LocalDateTime getMonthEnd(YearMonth month, LocalDate requestedEnd) {
        LocalDateTime monthEnd = month.atEndOfMonth().atTime(LocalTime.MAX);
        return monthEnd.isAfter(requestedEnd.atTime(LocalTime.MAX)) ? requestedEnd.atTime(LocalTime.MAX) : monthEnd;
    }
}
