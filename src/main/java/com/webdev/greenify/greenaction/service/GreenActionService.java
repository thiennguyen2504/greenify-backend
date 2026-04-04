package com.webdev.greenify.greenaction.service;

import com.webdev.greenify.greenaction.dto.request.CreateGreenActionPostRequest;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostDetailResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostSummaryResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;

import java.time.LocalDate;
import java.util.List;

public interface GreenActionService {

    GreenActionPostDetailResponse createPost(CreateGreenActionPostRequest request);

    List<GreenActionPostSummaryResponse> getTopPosts(int limit);

    PagedResponse<GreenActionPostSummaryResponse> getPostsByFilter(
            String actionTypeId,
            String groupName,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size);

    GreenActionPostDetailResponse getPostDetail(String postId);
}
