package com.webdev.greenify.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRolesRequestDTO {
    @NotEmpty(message = "Danh sách vai trò không được để trống")
    private Set<String> roleNames;
}
