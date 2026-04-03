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
    @NotEmpty(message = "Role names cannot be empty")
    private Set<String> roleNames;
}
