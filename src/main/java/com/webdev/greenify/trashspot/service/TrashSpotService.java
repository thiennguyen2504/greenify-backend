package com.webdev.greenify.trashspot.service;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.trashspot.dto.request.CreateResolveRequestRequest;
import com.webdev.greenify.trashspot.dto.request.CreateTrashSpotRequest;
import com.webdev.greenify.trashspot.dto.request.CreateTrashSpotReportRequest;
import com.webdev.greenify.trashspot.dto.request.ReviewResolveRequest;
import com.webdev.greenify.trashspot.dto.request.SubmitVerificationRequest;
import com.webdev.greenify.trashspot.dto.response.ResolveRequestResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotDetailResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotReportResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotSummaryResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotVerificationResponse;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import com.webdev.greenify.trashspot.enumeration.SeverityTier;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;

import java.util.List;

public interface TrashSpotService {

    CreateOrMergeResult createOrMerge(CreateTrashSpotRequest request);

    List<TrashSpotSummaryResponse> getTrashSpots(
            String province,
            TrashSpotStatus status,
            SeverityTier severity,
            String wasteTypeId);

    TrashSpotDetailResponse getTrashSpotDetail(String id);

    TrashSpotVerificationResponse submitVerification(String id, SubmitVerificationRequest request);

        TrashSpotReportResponse reportTrashSpot(String id, CreateTrashSpotReportRequest request);

    List<TrashSpotSummaryResponse> getNgoTrashSpots(String province, SeverityTier severity, String wasteTypeId);

    TrashSpotDetailResponse claimSpot(String id);

    ResolveRequestResponse createResolveRequest(String id, CreateResolveRequestRequest request);

    List<TrashSpotSummaryResponse> getAdminTrashSpots(
            TrashSpotStatus status,
            String province,
            SeverityTier severity,
            String wasteTypeId);

    PagedResponse<ResolveRequestResponse> getResolveRequests(ResolveRequestStatus status, int page, int size);

        PagedResponse<TrashSpotReportResponse> getTrashSpotReports(int page, int size);

    ResolveRequestResponse approveResolveRequest(String resolveRequestId);

    ResolveRequestResponse rejectResolveRequest(String resolveRequestId, ReviewResolveRequest request);

    TrashSpotDetailResponse reopenResolvedSpot(String id);

        void deleteTrashSpot(String id);

    int recalculateAllActiveHotScores();

    void invalidateActionTypeCache();

    record CreateOrMergeResult(TrashSpotDetailResponse response, boolean created) {
    }
}
