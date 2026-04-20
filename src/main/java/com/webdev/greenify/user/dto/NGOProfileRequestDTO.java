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
    @NotBlank(message = "Tên tổ chức là bắt buộc")
    String orgName;

    @NotBlank(message = "Tên người đại diện là bắt buộc")
    String representativeName;

    @NotBlank(message = "Hotline là bắt buộc")
    String hotline;

    @NotBlank(message = "Email liên hệ là bắt buộc")
    @Email(message = "Định dạng email không hợp lệ")
    String contactEmail;

    String description;

    @Valid
    AddressRequestDTO address;

    @Valid
    ImageRequestDTO avatar;

    @NotEmpty(message = "Cần ít nhất một tài liệu xác minh")
    @Valid
    List<ImageRequestDTO> verificationDocs;
}
