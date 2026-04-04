package com.webdev.greenify.file.service;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    ImageRequestDTO uploadImage(MultipartFile file);
}

