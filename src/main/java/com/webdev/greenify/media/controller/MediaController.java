package com.webdev.greenify.media.controller;

import com.webdev.greenify.media.dto.request.MediaUploadRequest;
import com.webdev.greenify.media.dto.response.MediaUploadResponse;
import com.webdev.greenify.media.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('USER', 'CTV', 'ADMIN')")
    public ResponseEntity<MediaUploadResponse> uploadMedia(@Valid @RequestBody MediaUploadRequest request) {
        return ResponseEntity.ok(mediaService.uploadMedia(request));
    }
}
