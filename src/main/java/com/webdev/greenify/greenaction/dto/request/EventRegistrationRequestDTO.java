package com.webdev.greenify.greenaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationRequestDTO {
    @NotBlank(message = "Event ID là bắt buộc")
    private String eventId;
    private String note;
}
