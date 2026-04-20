package com.webdev.greenify.voucher.dto.request;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherTemplateRequest {

    @NotBlank(message = "Tên voucher là bắt buộc")
    private String name;

    @NotBlank(message = "Tên đối tác là bắt buộc")
    private String partnerName;

    private String description;

    @NotNull(message = "Điểm yêu cầu là bắt buộc")
    @DecimalMin(value = "0.01", message = "Điểm yêu cầu phải lớn hơn 0")
    private BigDecimal requiredPoints;

    @NotNull(message = "Tổng số lượng là bắt buộc")
    @Min(value = 1, message = "Tổng số lượng phải lớn hơn 0")
    private Integer totalStock;

    private String usageConditions;

    @NotNull(message = "Thời hạn hiệu lực là bắt buộc")
    @Future(message = "Thời hạn hiệu lực phải ở tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime validUntil;

    @Valid
    private ImageRequestDTO partnerLogo;

    @Valid
    private ImageRequestDTO thumbnail;
}