package com.webdev.greenify.user.dto;

import com.webdev.greenify.file.dto.ImageResponseDTO;
import com.webdev.greenify.station.dto.AddressResponseDTO;
import com.webdev.greenify.user.enumeration.NGOProfileStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NGOProfileResponseDTO {
    String id;
    String orgName;
    String representativeName;
    String hotline;
    String contactEmail;
    String description;
    NGOProfileStatus status;
    String rejectedReason;
    Integer rejectedCount;
    AddressResponseDTO address;
    ImageResponseDTO avatar;
    List<ImageResponseDTO> verificationDocs;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
