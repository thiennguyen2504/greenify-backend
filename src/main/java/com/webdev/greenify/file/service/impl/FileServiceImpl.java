package com.webdev.greenify.file.service.impl;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    @Override
    public ImageRequestDTO uploadImage(MultipartFile file) {
        // MOCK UPLOAD TO S3
        String filename = file.getOriginalFilename();
        String objectKey = UUID.randomUUID().toString() + "_" + filename;
        
        return ImageRequestDTO.builder()
                .bucketName("greenify-bucket")
                .objectKey(objectKey)
                .imageUrl("https://greenify-bucket.s3.amazonaws.com/" + objectKey)
                .build();
    }
}

