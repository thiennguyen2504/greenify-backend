package com.webdev.greenify.trashspot.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.webdev.greenify.file.dto.ImageRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateResolveRequestRequest {

    @Valid
    @NotEmpty(message = "Ít nhất một ảnh là bắt buộc")
    private List<ImageRequestDTO> images;

    @NotNull(message = "Thời gian dọn dẹp là bắt buộc")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cleanedAt;

    @NotBlank(message = "Mô tả là bắt buộc")
    private String description;
}
