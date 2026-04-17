package com.webdev.greenify.user.dto;

import com.webdev.greenify.file.dto.ImageResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NGOPreviewDTO {
    private String id;
    private String name;
    private ImageResponseDTO avatar;
}
