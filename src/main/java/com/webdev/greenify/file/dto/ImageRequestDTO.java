package com.webdev.greenify.file.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequestDTO {
    @NotBlank(message = "Tên bucket là bắt buộc")
    private String bucketName;

    @NotBlank(message = "Object key là bắt buộc")
    private String objectKey;

    @NotBlank(message = "URL ảnh là bắt buộc")
    private String imageUrl;
}

