package com.webdev.greenify.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailResponseDTO {
    private String id;
    private String email;
    private String phoneNumber;
    private String username;
    private Set<String> roles;
    private UserProfileResponseDTO userProfile;
}
