package com.webdev.greenify.media.service.impl;

import com.webdev.greenify.media.dto.request.MediaUploadRequest;
import com.webdev.greenify.media.dto.response.MediaUploadResponse;
import com.webdev.greenify.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    @Override
    public MediaUploadResponse uploadMedia(MediaUploadRequest request) {
        // TODO: Replace with S3 upload logic
        log.info("Media upload requested with URL: {}", request.getUrl());
        
        return MediaUploadResponse.builder()
                .mediaUrl(request.getUrl())
                .build();
    }
}
