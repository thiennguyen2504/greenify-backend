package com.webdev.greenify.user.dto;

import com.webdev.greenify.user.enumeration.UserProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponseDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String displayName;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private UserProfileStatus status;
    private String avatarUrl;
}
