package com.webdev.greenify.file.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageRequestDTO {
    @NotBlank(message = "Bucket name is required")
    private String bucketName;

    @NotBlank(message = "Object key is required")
    private String objectKey;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;
}

