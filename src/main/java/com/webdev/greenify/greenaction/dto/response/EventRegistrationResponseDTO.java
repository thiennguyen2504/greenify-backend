package com.webdev.greenify.greenaction.dto.response;

import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import com.webdev.greenify.user.dto.UserProfileResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationResponseDTO {
    private String id;
    private String eventId;
    private String eventTitle;
    private String userId;
    private String username;
    private RegistrationStatus status;
    private LocalDateTime createdAt;
    private String note;
    private String registrationCode;
    private UserProfileResponseDTO userProfile;
}
