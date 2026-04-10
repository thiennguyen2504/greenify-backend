package com.webdev.greenify.voucher.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherTemplateRequest {

    @NotBlank(message = "Voucher name is required")
    private String name;

    @NotBlank(message = "Partner name is required")
    private String partnerName;

    private String description;

    @NotNull(message = "Required points is required")
    @DecimalMin(value = "0.01", message = "Required points must be greater than 0")
    private BigDecimal requiredPoints;

    @NotNull(message = "Total stock is required")
    @Min(value = 1, message = "Total stock must be greater than 0")
    private Integer totalStock;

    private String usageConditions;

    @NotNull(message = "Valid until is required")
    @Future(message = "Valid until must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime validUntil;

    private MultipartFile partnerLogo;

    private MultipartFile thumbnail;
}