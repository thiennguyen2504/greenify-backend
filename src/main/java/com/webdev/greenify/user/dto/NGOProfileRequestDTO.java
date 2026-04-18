package com.webdev.greenify.user.dto;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.station.dto.AddressRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NGOProfileRequestDTO {
    @NotBlank(message = "Organization name is required")
    String orgName;

    @NotBlank(message = "Representative name is required")
    String representativeName;

    @NotBlank(message = "Hotline is required")
    String hotline;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    String contactEmail;

    String description;

    @Valid
    AddressRequestDTO address;

    @Valid
    ImageRequestDTO avatar;

    @NotEmpty(message = "At least one verification document is required")
    @Valid
    List<ImageRequestDTO> verificationDocs;
}
