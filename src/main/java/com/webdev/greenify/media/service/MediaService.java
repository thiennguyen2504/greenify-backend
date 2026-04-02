package com.webdev.greenify.media.service;

import com.webdev.greenify.media.dto.request.MediaUploadRequest;
import com.webdev.greenify.media.dto.response.MediaUploadResponse;

public interface MediaService {

    MediaUploadResponse uploadMedia(MediaUploadRequest request);
}
