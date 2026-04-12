package com.webdev.greenify.greenaction.dto.response;

import com.webdev.greenify.file.enumeration.EventImageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventImageResponseDTO {
    private String id;
    private String objectKey;
    private String bucketName;
    private String imageUrl;
    private EventImageType imageType;
}
