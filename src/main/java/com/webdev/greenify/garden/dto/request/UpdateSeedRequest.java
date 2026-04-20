package com.webdev.greenify.garden.dto.request;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.garden.enumeration.PlantCycleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSeedRequest {

    @Size(max = 100, message = "Tên hạt giống không được vượt quá 100 ký tự")
    private String name;

    @Valid
    private ImageRequestDTO stage1Image;

    @Valid
    private ImageRequestDTO stage2Image;

    @Valid
    private ImageRequestDTO stage3Image;

    @Valid
    private ImageRequestDTO stage4Image;

    @Positive(message = "Số ngày trưởng thành phải lớn hơn 0")
    private Integer daysToMature;

    @Positive(message = "Ngày bắt đầu giai đoạn 2 phải lớn hơn 0")
    private Integer stage2FromDay;

    @Positive(message = "Ngày bắt đầu giai đoạn 3 phải lớn hơn 0")
    private Integer stage3FromDay;

    @Positive(message = "Ngày bắt đầu giai đoạn 4 phải lớn hơn 0")
    private Integer stage4FromDay;

    private PlantCycleType cycleType;

    private String rewardVoucherTemplateId;

    private Boolean isActive;
}
