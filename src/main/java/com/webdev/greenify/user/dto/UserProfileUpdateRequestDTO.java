package com.webdev.greenify.user.dto;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileUpdateRequestDTO {
    private String firstName;
    private String lastName;
    @NotBlank(message = "Tên hiển thị là bắt buộc")
    private String displayName;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    @Valid
    private ImageRequestDTO avatar; 
}

