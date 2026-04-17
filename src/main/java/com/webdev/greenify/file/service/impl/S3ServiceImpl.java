package com.webdev.greenify.file.service.impl;

import com.webdev.greenify.file.dto.ImageResponseDTO;
import com.webdev.greenify.file.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements UploadService {
    private final S3Client s3Client;

    @Value("${domain.cloud-front}")
    private String domainCloudFront;

    @Value("${storage.bucket-name}")
    private String bucketName;

    @Override
    public List<ImageResponseDTO> uploadImage(List<MultipartFile> files) {
        return files.stream().map(file -> {
            String objectKey = uploadFile(file);
            ImageResponseDTO imageResponseDTO = new ImageResponseDTO();
            imageResponseDTO.setObjectKey(objectKey);
            imageResponseDTO.setBucketName(bucketName);
            imageResponseDTO.setImageUrl(domainCloudFront + objectKey);
            return imageResponseDTO;
        }).toList();
    }

    private String uploadFile(MultipartFile file) {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String imageFolder = "images";
        String objectKey = imageFolder + "/" + fileName;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new RuntimeException("Error uploading file: " + fileName, e);
        }

        return objectKey;
    }
}
