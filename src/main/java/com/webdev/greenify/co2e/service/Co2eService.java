package com.webdev.greenify.co2e.service;

import com.webdev.greenify.co2e.dto.response.Co2eTransactionResponse;
import com.webdev.greenify.co2e.dto.response.GreenImpactWalletResponse;
import com.webdev.greenify.co2e.dto.response.PostCo2eResponse;
import com.webdev.greenify.co2e.event.Co2eAnalysisEvent;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;

/**
 * Business orchestration for CO2e analysis and wallet APIs.
 */
public interface Co2eService {

    void processPostCo2e(Co2eAnalysisEvent event);

    GreenImpactWalletResponse getWalletForCurrentUser();

    PagedResponse<Co2eTransactionResponse> getCo2eHistoryForCurrentUser(int page, int size);

    PostCo2eResponse getCo2eForPost(String postId);
}
