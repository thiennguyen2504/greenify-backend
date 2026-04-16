package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.CreateAppealRequest;
import com.webdev.greenify.greenaction.dto.request.ReviewAppealRequest;
import com.webdev.greenify.greenaction.dto.request.UpdateAppealRequest;
import com.webdev.greenify.greenaction.dto.response.AppealResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.AppealStatus;

public interface AppealService {

    AppealResponse createAppeal(CreateAppealRequest request);

    PagedResponse<AppealResponse> getAppealsForReview(AppealStatus status, int page, int size);

    AppealResponse getAppealDetail(String appealId);

    AppealResponse updateAppeal(String appealId, UpdateAppealRequest request);

    AppealResponse reviewAppeal(String appealId, ReviewAppealRequest request);
}
