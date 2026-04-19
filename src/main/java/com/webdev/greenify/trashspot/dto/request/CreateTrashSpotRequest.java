package com.webdev.greenify.trashspot.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.webdev.greenify.file.dto.ImageRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTrashSpotRequest {

    @Valid
    @NotEmpty(message = "Ít nhất một ảnh là bắt buộc")
    private List<ImageRequestDTO> images;

    @NotNull(message = "Latitude là bắt buộc")
    private BigDecimal latitude;

    @NotNull(message = "Longitude là bắt buộc")
    @JsonAlias("longtitude")
    private BigDecimal longitude;

    @NotBlank(message = "Tỉnh/thành là bắt buộc")
    private String province;

    @Size(max = 255, message = "Tên điểm rác tối đa 255 ký tự")
    private String name;

    @NotEmpty(message = "Ít nhất một loại rác là bắt buộc")
    private List<String> wasteTypeIds;

    @NotBlank(message = "Mô tả là bắt buộc")
    private String description;
}
