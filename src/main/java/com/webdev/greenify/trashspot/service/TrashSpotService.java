package com.webdev.greenify.trashspot.service;

import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.trashspot.dto.request.CreateResolveRequestRequest;
import com.webdev.greenify.trashspot.dto.request.CreateTrashSpotRequest;
import com.webdev.greenify.trashspot.dto.request.ReviewResolveRequest;
import com.webdev.greenify.trashspot.dto.request.SubmitVerificationRequest;
import com.webdev.greenify.trashspot.dto.response.ResolveRequestResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotDetailResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotSummaryResponse;
import com.webdev.greenify.trashspot.dto.response.TrashSpotVerificationResponse;
import com.webdev.greenify.trashspot.enumeration.ResolveRequestStatus;
import com.webdev.greenify.trashspot.enumeration.TrashSpotStatus;

public interface TrashSpotService {

    CreateOrMergeResult createOrMerge(CreateTrashSpotRequest request);

    PagedResponse<TrashSpotSummaryResponse> getTrashSpots(
            String province,
            TrashSpotStatus status,
            int page,
            int size);

    TrashSpotDetailResponse getTrashSpotDetail(String id);

    TrashSpotVerificationResponse submitVerification(String id, SubmitVerificationRequest request);

    PagedResponse<TrashSpotSummaryResponse> getNgoTrashSpots(String province, int page, int size);

    TrashSpotDetailResponse claimSpot(String id);

    ResolveRequestResponse createResolveRequest(String id, CreateResolveRequestRequest request);

    PagedResponse<TrashSpotSummaryResponse> getAdminTrashSpots(
            TrashSpotStatus status,
            String province,
            int page,
            int size);

    PagedResponse<ResolveRequestResponse> getResolveRequests(ResolveRequestStatus status, int page, int size);

    ResolveRequestResponse approveResolveRequest(String resolveRequestId);

    ResolveRequestResponse rejectResolveRequest(String resolveRequestId, ReviewResolveRequest request);

    TrashSpotDetailResponse reopenResolvedSpot(String id);

    int recalculateAllActiveHotScores();

        void invalidateActionTypeCache();

    record CreateOrMergeResult(TrashSpotDetailResponse response, boolean created) {
    }
}
