package com.webdev.greenify.greenaction.controller;

import com.webdev.greenify.greenaction.dto.request.CreateGreenActionPostRequest;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostDetailResponse;
import com.webdev.greenify.greenaction.dto.response.GreenActionPostSummaryResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;
import com.webdev.greenify.greenaction.enumeration.PostStatus;
import com.webdev.greenify.greenaction.service.GreenActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/green-action/posts")
@RequiredArgsConstructor
public class GreenActionController {

    private final GreenActionService greenActionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
    public ResponseEntity<GreenActionPostDetailResponse> createPost(
            @Valid @RequestBody CreateGreenActionPostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(greenActionService.createPost(request));
    }

    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<List<GreenActionPostSummaryResponse>> getTopPosts(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(greenActionService.getTopPosts(limit));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<PagedResponse<GreenActionPostSummaryResponse>> getPostsByFilter(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) String actionTypeId,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(greenActionService.getPostsByFilter(
                status, actionTypeId, groupName, fromDate, toDate, page, size));
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
    public ResponseEntity<PagedResponse<GreenActionPostSummaryResponse>> getMyPostHistory(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(greenActionService.getPostHistoryForCurrentUser(
                status, fromDate, toDate, page, size));
    }

    @GetMapping("/{postId}")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN', 'NGO')")
    public ResponseEntity<GreenActionPostDetailResponse> getPostDetail(@PathVariable String postId) {
        return ResponseEntity.ok(greenActionService.getPostDetail(postId));
    }
}
