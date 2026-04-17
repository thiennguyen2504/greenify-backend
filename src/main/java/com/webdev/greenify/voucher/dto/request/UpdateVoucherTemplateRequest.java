package com.webdev.greenify.voucher.dto.request;

import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.voucher.enumeration.VoucherTemplateStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoucherTemplateRequest {

    @Size(max = 200, message = "Voucher name must not exceed 200 characters")
    private String name;

    @Size(max = 200, message = "Partner name must not exceed 200 characters")
    private String partnerName;

    private String description;

    @DecimalMin(value = "0.01", message = "Required points must be greater than 0")
    private BigDecimal requiredPoints;

    @Min(value = 1, message = "Additional stock must be greater than 0")
    private Integer additionalStock;

    private String usageConditions;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime validUntil;

    private VoucherTemplateStatus status;

    @Valid
    private ImageRequestDTO partnerLogo;

    @Valid
    private ImageRequestDTO thumbnail;
}
