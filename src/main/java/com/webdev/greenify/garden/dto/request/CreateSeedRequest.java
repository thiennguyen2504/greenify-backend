package com.webdev.greenify.garden.dto.request;

import com.webdev.greenify.garden.enumeration.PlantCycleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeedRequest {

    @NotBlank(message = "Tên hạt giống là bắt buộc")
    private String name;

    @NotBlank(message = "URL ảnh giai đoạn 1 là bắt buộc")
    private String stage1ImageUrl;

    @NotBlank(message = "URL ảnh giai đoạn 2 là bắt buộc")
    private String stage2ImageUrl;

    @NotBlank(message = "URL ảnh giai đoạn 3 là bắt buộc")
    private String stage3ImageUrl;

    @NotBlank(message = "URL ảnh giai đoạn 4 là bắt buộc")
    private String stage4ImageUrl;

    @NotNull(message = "Số ngày trưởng thành là bắt buộc")
    @Positive(message = "Số ngày trưởng thành phải lớn hơn 0")
    private Integer daysToMature;

    @NotNull(message = "Ngày bắt đầu giai đoạn 2 là bắt buộc")
    @Positive(message = "Ngày bắt đầu giai đoạn 2 phải lớn hơn 0")
    private Integer stage2FromDay;

    @NotNull(message = "Ngày bắt đầu giai đoạn 3 là bắt buộc")
    @Positive(message = "Ngày bắt đầu giai đoạn 3 phải lớn hơn 0")
    private Integer stage3FromDay;

    @NotNull(message = "Ngày bắt đầu giai đoạn 4 là bắt buộc")
    @Positive(message = "Ngày bắt đầu giai đoạn 4 phải lớn hơn 0")
    private Integer stage4FromDay;

    @NotNull(message = "Loại chu kỳ cây là bắt buộc")
    private PlantCycleType cycleType;

    private String rewardVoucherTemplateId;
}
