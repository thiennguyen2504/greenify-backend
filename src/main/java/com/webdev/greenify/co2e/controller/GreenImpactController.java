package com.webdev.greenify.co2e.controller;

import com.webdev.greenify.co2e.dto.response.Co2eTransactionResponse;
import com.webdev.greenify.co2e.dto.response.GreenImpactWalletResponse;
import com.webdev.greenify.co2e.dto.response.PostCo2eResponse;
import com.webdev.greenify.co2e.service.Co2eService;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GreenImpactController {

    private final Co2eService co2eService;

    @GetMapping("/wallet/co2e")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<GreenImpactWalletResponse> getMyGreenImpactWallet() {
        return ResponseEntity.ok(co2eService.getWalletForCurrentUser());
    }

    @GetMapping("/me/co2e-history")
    @PreAuthorize("hasAnyRole('USER', 'CTV')")
    public ResponseEntity<PagedResponse<Co2eTransactionResponse>> getMyCo2eHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(co2eService.getCo2eHistoryForCurrentUser(page, size));
    }

    @GetMapping("/posts/{postId}/co2e")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
    public ResponseEntity<PostCo2eResponse> getPostCo2e(@PathVariable String postId) {
        return ResponseEntity.ok(co2eService.getCo2eForPost(postId));
    }
}
