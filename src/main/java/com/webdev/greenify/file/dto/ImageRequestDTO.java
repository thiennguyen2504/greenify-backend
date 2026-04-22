package com.webdev.greenify.file.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageRequestDTO {
    @NotBlank(message = "Tên bucket là bắt buộc")
    private String bucketName;

    @NotBlank(message = "Object key là bắt buộc")
    private String objectKey;

    @NotBlank(message = "URL ảnh là bắt buộc")
    private String imageUrl;
}

