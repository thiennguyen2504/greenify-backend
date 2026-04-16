package com.webdev.greenify.file.dto;

import lombok.Data;

@Data
public class ImageResponseDTO {
    private String bucketName;
    private String objectKey;
    private String imageUrl;
}
