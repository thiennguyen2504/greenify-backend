package com.webdev.greenify.trashspot.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTrashSpotReportRequest {

    @NotBlank(message = "Nội dung báo cáo là bắt buộc")
    @Size(max = 1000, message = "Nội dung báo cáo tối đa 1000 ký tự")
    private String note;
}